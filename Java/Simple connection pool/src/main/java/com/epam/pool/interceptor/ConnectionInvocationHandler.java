package com.epam.pool.interceptor;

import com.epam.pool.ConnectionPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

public class ConnectionInvocationHandler implements InvocationHandler {
    private static Logger logger = LogManager.getLogger(ConnectionInvocationHandler.class);

    /**
     * For return connection to the connection pool
     */
    private ConnectionPool pool;
    /**
     * Original connection for usage of methods
     */
    private Connection connection;

    public ConnectionInvocationHandler(ConnectionPool pool, Connection connection) {
        this.pool = pool;
        this.connection = connection;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // если пул не закрыт - перехватываем close
        if (!pool.isClosed() && "close".equals(method.getName())) {
            onClose((Connection) proxy);
            return null;
        }
        if ("equals".equals(method.getName())) {
            if (Proxy.isProxyClass(args[0].getClass())) {
                InvocationHandler invHandler = Proxy.getInvocationHandler(args[0]);
                if (invHandler instanceof ConnectionInvocationHandler) {
                    ConnectionInvocationHandler ch = (ConnectionInvocationHandler) invHandler;
                    return connection.equals(ch.connection);
                }
            }
        }
        return method.invoke(connection, args);
    }

    @SuppressWarnings("unchecked")
    private void onClose(Connection proxy) throws SQLException {
        // если идет очищение, то закрываем соединение, иначе - возвращаем в пул
        if (pool.isClean()) {
            logger.trace("Connection was closed by pool cleaner");
            connection.close();
        } else {
            logger.trace("Connection was released by invocation handler");
            pool.release(proxy);
        }
    }
}
