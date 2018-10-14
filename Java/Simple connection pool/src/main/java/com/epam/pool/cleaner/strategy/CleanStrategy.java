package com.epam.pool.cleaner.strategy;

import com.epam.pool.ConnectionPool;

public interface CleanStrategy {
    void clean(ConnectionPool pool);
}
