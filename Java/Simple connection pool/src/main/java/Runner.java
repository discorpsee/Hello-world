import com.epam.pool.ConnectionPool;
import com.epam.settings.ConfigurationBuilder;
import com.epam.settings.PoolConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Runner {
    private static Logger logger = LogManager.getLogger(Runner.class);

    public static void main(String[] args) throws Exception {
        logger.info("System started");
        PoolConfiguration poolConfiguration = ConfigurationBuilder.start()
                .url("jdbc:mysql://localhost/photostudio")
                .driver("com.mysql.jdbc.Driver")
                .user("test")
                .password("11222333s4v5")
                .minSize(1)
                .maxSize(10)
                .maxWait(1000)
                .sweeper(true)
                .timeBetweenEvictionRuns(1, TimeUnit.SECONDS)
                .forceClose(true)
                .returnOnClose(true)
                .build();

        System.out.println(poolConfiguration);

        ConnectionPool<Connection> connectionPool = new ConnectionPool<>(poolConfiguration);
        Connection connection = connectionPool.get();

        for (int i = 0; i < 10; i++) {
            Connection connection1 = connectionPool.get();
            Connection connection2 = connectionPool.get();
            Connection connection3 = connectionPool.get();
            connectionPool.release(connection1);
            connectionPool.release(connection2);
            connectionPool.release(connection3);
        }

        Thread.sleep(3000);
        logger.info("System stopped");
        connectionPool.close();
    }

    public static class ConnectionUser extends Thread {
        private ConnectionPool connectionPool;

        @Override
        public void run() {
            try {
                Connection connection = connectionPool.get();
                System.out.println("I took connection");
                Thread.sleep(100);
                connectionPool.release(connection);
                System.out.println("I released connection");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
