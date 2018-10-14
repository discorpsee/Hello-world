package com.epam.pool.cleaner.strategy;

import com.epam.pool.ConnectionPool;
import com.epam.settings.PoolConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.IntSummaryStatistics;

public class AverageBusyStrategy implements CleanStrategy {
    private static Logger logger = LogManager.getLogger(AverageBusyStrategy.class);
    private IntSummaryStatistics busyStatistics;

    public AverageBusyStrategy() {
        busyStatistics = new IntSummaryStatistics();
    }

    @Override
    public void clean(ConnectionPool pool) {
        PoolConfiguration config = pool.getConfig();
        int idle = pool.getIdle().size();
        int size = pool.getSize().get();
        // собираем статистику по занятым соединениям
        busyStatistics.accept(size - idle);
        int minSize = config.getMinSize();
        int busy = size - idle;
        logger.debug("Idle.size = {}, connections total = {}, in use {} connection(s)", idle, size, busy);
        // если размер больше минимального
        // и есть свободные соединения
        // то есть смысл вобще что-то делать
        if (size > minSize && idle > 0) {
            checkBusyAverage(size, pool, idle);
        } else {
            logger.debug("Size {} == minSize {}, I cannot clean the pool", size, minSize);
        }
    }

    private void checkBusyAverage(int poolSize, ConnectionPool pool, int idle) {
        // если размер больше среднего использования
        // тогда есть что почистить
        double busyAverage = busyStatistics.getAverage();
        if (poolSize > busyAverage) {
            cleanConnections(poolSize, pool, busyAverage, idle);
        } else {
            logger.debug("PoolSize {} <= busyAverage {}, We cannot clean the pool", poolSize, busyAverage);
        }
    }

    private void cleanConnections(int poolSize, ConnectionPool pool, double busyAverage, int idle) {
        // можно чистить
        double shallClean = poolSize - busyAverage;
        logger.debug("PoolSize {} > busyAverage {}, We shall clean {} connection(s)", poolSize, busyAverage, shallClean);
        logger.info("Shall clean {} connections", shallClean);
        // можем чистить только свободные соединения
        if (shallClean > idle) {
            logger.trace("ShallClean > idle, set shallClean to {}, cause we cant clean busy connections", idle);
            shallClean = idle;
        }
        pool.releaseExtraConnections((int) shallClean); //TODO: доделать
    }
}
