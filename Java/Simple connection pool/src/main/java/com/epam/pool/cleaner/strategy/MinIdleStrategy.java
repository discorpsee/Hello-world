package com.epam.pool.cleaner.strategy;

import com.epam.pool.ConnectionPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MinIdleStrategy implements CleanStrategy {
    private static Logger logger = LogManager.getLogger(MinIdleStrategy.class);

    @Override
    public void clean(ConnectionPool pool) {
        int minSize = pool.getConfig().getMinSize();
        int idle = pool.getIdle().size();
        if (idle > minSize) {
            logger.debug("idle {} > minSize {}", idle, minSize);
            int shallClean = idle - minSize;
            if (shallClean > 0) {
                pool.releaseExtraConnections(shallClean);
            }
            logger.debug("Shall clean {} connections", shallClean);
        }
    }
}
