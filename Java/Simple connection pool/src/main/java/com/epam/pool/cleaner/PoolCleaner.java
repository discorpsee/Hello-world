package com.epam.pool.cleaner;


import com.epam.pool.ConnectionPool;
import com.epam.pool.cleaner.strategy.CleanStrategy;
import com.epam.pool.cleaner.strategy.MinIdleStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.ref.WeakReference;
import java.util.TimerTask;

/**
 * http://grepcode.com/file/repo1.maven.org/maven2/org.apache.tomcat/tomcat-jdbc/8.0.24/org/apache/tomcat/jdbc/pool/ConnectionPool.java#ConnectionPool.PoolCleaner
 */
public class PoolCleaner extends TimerTask {
    private static Logger logger = LogManager.getLogger(PoolCleaner.class);
    /**
     * Интервал прочистки 30 секунд
     */
    private static final int DEFAULT_INTERVAL = 30;
    /**
     * Миллисекунд в секунде
     */
    private static final int MILLISECONDS_IN_SECOND = 1000;

    private WeakReference<ConnectionPool> pool;
    private long sleepTime;
    private CleanStrategy cleaner;

    public PoolCleaner(WeakReference<ConnectionPool> pool, long sleepTime, CleanStrategy cleaner) {
        this.pool = pool;
        this.sleepTime = sleepTime;
        this.cleaner = cleaner;
        checkSleepTime(sleepTime);
    }

    public PoolCleaner(ConnectionPool pool, long sleepTime) {
        this.pool = new WeakReference<>(pool);
        this.sleepTime = sleepTime;
        this.cleaner = new MinIdleStrategy();
        checkSleepTime(sleepTime);
    }

    private void checkSleepTime(long sleepTime) {
        if (sleepTime <= 0) {
            logger.warn("Database connection pool evicter thread interval is set to 0, defaulting to 30 seconds");
            this.sleepTime = MILLISECONDS_IN_SECOND * DEFAULT_INTERVAL;
        } else if (sleepTime < 1000) {
            logger.warn("Database connection pool evicter thread interval is set to lower than 1 second.");
        }
    }

    public void run() {
        logger.trace("PoolCleaner has been running");
        ConnectionPool pool = this.pool.get();
        if (pool == null) {
            logger.debug("PoolConnection is null, so PollCleaner will be cancelled");
            stopRunning();
            return;
        }
        // если не закрыт - пробуем почистить
        if (!pool.isClosed()) {
            cleaner.clean(pool);
        }
    }

    public long getSleepTime() {
        return sleepTime;
    }

    public void start() {
        ConnectionPool pool = this.pool.get();
        if (pool != null) {
            logger.debug("Try to register PoolCleaner");
            pool.registerCleaner(this);
        }
    }

    public void stopRunning() {
        this.cancel();
        ConnectionPool pool = this.pool.get();
        if (pool != null) {
            pool.unregisterCleaner(this);
        }
    }

    @Override
    public String toString() {
        return "PoolCleaner{" +
                "pool=" + pool +
                ", sleepTime=" + sleepTime +
                "}";
    }
}
