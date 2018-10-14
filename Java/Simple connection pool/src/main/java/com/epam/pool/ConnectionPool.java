package com.epam.pool;


import com.epam.pool.cleaner.PoolCleaner;
import com.epam.pool.exception.WrongConnectionsCount;
import com.epam.pool.interceptor.ConnectionInvocationHandler;
import com.epam.settings.PoolConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Общие мысли:
 * По-хорошему необходимо отслеживать сколько соедидение бездействует
 *
 * @param <T>
 */
public class ConnectionPool<T extends java.sql.Connection> implements ObjectPool<T>, AutoCloseable {
    private static Logger logger = LogManager.getLogger(ConnectionPool.class);

    /**
     * All the information about the connection pool
     * These are the properties the pool got instantiated with
     */
    private PoolConfiguration config;
    /**
     * Carries the size of the pool,
     * instead of relying on a queue implementation that usually iterates over to get an exact count
     */
    private AtomicInteger size = new AtomicInteger(0);
    /**
     * Contains all the connections that are in use
     * TODO - this shouldn't be a blocking queue, simply a list to hold our objects
     */
    private BlockingQueue<T> busy;
    /**
     * Contains all the idle connections
     */
    private BlockingQueue<T> idle;
    /**
     * Pool closed flag
     */
    private volatile boolean closed = false;
    /**
     * Counter to track how many threads are waiting for a connection
     */
    private AtomicInteger waitCount = new AtomicInteger(0);
    /**
     * Служит флагом для прокси, чтобы можно было очищать соединения
     */
    private volatile boolean clean;

    public ConnectionPool() {
    }

    public ConnectionPool(PoolConfiguration config) throws Exception {
        this.config = config;
        init(config);
    }

    private void init(PoolConfiguration config) throws Exception {
        try {
            Class.forName(config.getDriver());
        } catch (ClassNotFoundException e) {
            logger.error("Cant find driver {}", config.getDriver());
            throw e;
        }
        if (config.getMaxSize() < 1) {
            logger.warn("maxSize is smaller than 1, setting maxActive to: {}", PoolConfiguration.DEFAULT_MAX_SIZE);
            config.setMaxSize(PoolConfiguration.DEFAULT_MAX_SIZE);
        }
        if (config.getMaxSize() < config.getMinSize()) {
            logger.warn("maxIdle is smaller than minIdle, setting maxIdle to: {}", config.getMinSize());
            config.setMaxSize(config.getMinSize());
        }
        //make space for 10 extra in case we flow over a bit
        this.busy = new ArrayBlockingQueue<>(config.getMaxSize(), false);
        //busy = new FairBlockingQueue<PooledConnection>();
        this.idle = new ArrayBlockingQueue<>(config.getMaxSize(), false); // TODO: is fair?

        initializePoolCleaner(config);
        initializeConnections(config);
    }

    private void initializeConnections(PoolConfiguration config) throws SQLException {
        logger.debug("Start connections initialization");
        int initialSize = config.getMinSize();
        for (int i = 0; i < initialSize; i++) {
            idle.add(getConnection());
        }
        logger.debug("End of connections initialization");
        logger.info("Created {} connections", initialSize);
    }

    private void initializePoolCleaner(PoolConfiguration config) {
        if (config.isSweeperEnabled()) {
            logger.debug("Pool cleaner is enabled");
            poolCleaner = new PoolCleaner(this, config.getTimeBetweenEvictionRunsMillis());
            poolCleaner.start();
            logger.info("PoolCleaner has been started");
        } else {
            logger.debug("Pool cleaner is disabled");
        }
    }

    public T get() throws Exception {
        T connection = idle.poll();
        if (connection != null) {
            busy.add(connection);
            return connection;
        }
        return borrowConnection();
    }

    public void release(T t) throws SQLException {
        terminateTransaction(t);
        busy.remove(t);
        releaseConnection(t);
    }

    private void releaseConnection(T t) throws SQLException {
        // если пул не закрыт или происходит очистка пула - возвращаем на место
        if (!isClosed() || isClean()) {
            idle.add(t);
        } else {
            // если закрыт - закрываем соединение
            t.close();
            size.decrementAndGet();
        }
    }

    private T borrowConnection() throws Exception {
        if (isClosed()) {
            logger.error("Connection pool closed.");
            throw new SQLException("Connection pool closed.");
        }
        long threadCome = System.currentTimeMillis();
        while (true) {
            // пытаемся получить свободное соединение
            T connection = idle.poll();
            // если оно есть - просто возвращаем
            if (connection != null) {
                return connection;
            }
            // если достигли размера - ждем
            if (size.get() == config.getMaxSize()) {
                // не использую wait из-за того что пришлось бы синхронизироваться на всем пуле (этом методе)
                // записываем поток в очередь
                waitCount.incrementAndGet();
                logger.trace("Thread {} are waiting for connection {} ms, waiting threads total {}",
                        Thread.currentThread().getName(), System.currentTimeMillis() - threadCome, waitCount.get());
                Thread.yield();
                // если время ожидания потока больше или равно заданному в конфиге - создаем новое соединение
            } else if (System.currentTimeMillis() - threadCome >= config.getMaxWait()) {
                logger.debug(String.format("Creation new connection for thread %s, after waiting %d ms, size = %d",
                        Thread.currentThread().getName(), System.currentTimeMillis() - threadCome, size.get()));
                // создаем новое соединение и возвращаем
                connection = getConnection(); // TODO: должно быть асинхронным
                busy.add(connection);
                logger.info("Created a connection, size = {}", size.get());
                // этот поток больше не ждет
                waitCount.decrementAndGet();
                return connection;
            }
        }
    }

    public void close() {
        this.closed = true;
        unregisterCleaner(this.poolCleaner);
        closeConnections(idle);
        logger.info("Closed idle connections");
        if (config.isForceClose()) {
            closeConnections(busy);
            logger.info("Closed busy connections");
        }
    }

    private void closeConnections(BlockingQueue<T> connections) {
        connections.forEach(this::closeConnection);
    }

    private void closeConnection(T connection) {
        try {
            logger.trace("Try to close Connection, {} left", size.get());
            connection.close();
            size.decrementAndGet();
            logger.debug("Connection closed, {} left", size.get());
        } catch (SQLException e) {
            logger.error("Cannot close connection {}, {}", connection, e);
        }
    }

    /**
     * Commit transaction
     *
     * @param connection
     * @throws SQLException
     */
    protected void terminateTransaction(Connection connection) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        if (!autoCommit) {
            connection.commit();
        }
    }

    public boolean isClosed() {
        return this.closed;
    }

    /**
     * Чтобы не создаавть поток на каждое соединение
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private void createConnectionAsync(int count) throws WrongConnectionsCount {
        if (count < 1) {
            logger.error("Wrong count of connections, got {}", count);
            throw new WrongConnectionsCount("Wrong count of connections, got " + count);
        }
        executor.submit(() -> {
            for (int i = 0; i < count; i++) {
                try {
                    idle.add(getConnection());
                } catch (SQLException e) {
                    logger.error("Cannot create connection, {}", e);
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private T getConnection() throws SQLException {
        logger.debug("Try to create a connection, current size = {}", size.get());
        T connection = (T) DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
        size.incrementAndGet();
        logger.debug("Connection created, total: {}", size.get());
        if (config.isReturnOnClose()) {
            logger.debug("Connection has been wrapped by proxy");
            return wrapByProxy(connection);
        }
        return connection;
    }

    @SuppressWarnings("unchecked")
    private T wrapByProxy(T t) {
        return (T) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(this, t));
    }

    /**
     * Таймер
     */
    private static volatile Timer poolCleanTimer = null;
    /**
     * Уборщик
     */
    private volatile PoolCleaner poolCleaner;
    /**
     * Зарегестрированные уборщики
     */
    private static HashSet<PoolCleaner> cleaners = new HashSet<>();

    public synchronized void registerCleaner(PoolCleaner cleaner) {
        logger.info("Register Pool cleaner {}", cleaner);
        unregisterCleaner(cleaner);
        cleaners.add(cleaner);
        if (poolCleanTimer == null) {
            poolCleanTimer = createTimerByPoolClassLoader();
        }
        poolCleanTimer.scheduleAtFixedRate(cleaner, cleaner.getSleepTime(), cleaner.getSleepTime());
        logger.info("Pool Cleaner {} has been scheduled", cleaner);
    }

    private Timer createTimerByPoolClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(ConnectionPool.class.getClassLoader());
            String timerName = String.format("Pool Cleaner [%s:%s]",
                    System.identityHashCode(ConnectionPool.class.getClassLoader()),
                    System.currentTimeMillis());
            return new Timer(timerName, true);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    public void unregisterCleaner(PoolCleaner cleaner) {
        boolean removed = cleaners.remove(cleaner);
        if (removed) {
            logger.debug("Unregister {}", cleaner);
            cleaner.cancel();
            if (poolCleanTimer != null) {
                poolCleanTimer.purge();
                if (cleaners.size() == 0) {
                    poolCleanTimer.cancel();
                    poolCleanTimer = null;
                }
            }
        }
    }

    public void releaseExtraConnections(int count) {
        logger.debug("Will release {} connection(s)", count);
        if (count > 0) {
            releaseExtra(count);
        } else {
            logger.error("Cannot release 0 connection(s)!");
        }
    }

    private void releaseExtra(int count) {
        clean = true;
        int released = 0;
        for (int i = 0; i < count; i++) {
            T poll = idle.poll();
            if (poll != null) {
                closeConnection(poll);
                released++;
            }
        }
        clean = false;
        logger.info("{} connection(s) have been released", released);
    }

    public AtomicInteger getSize() {
        return size;
    }

    public BlockingQueue<T> getBusy() {
        return busy;
    }

    public BlockingQueue<T> getIdle() {
        return idle;
    }

    public AtomicInteger getWaitCount() {
        return waitCount;
    }

    public PoolConfiguration getConfig() {
        return config;
    }

    public boolean isClean() {
        return clean;
    }
}
