package com.epam.pool;

public interface ObjectPool<T> {
    T get() throws Exception;
    void release(T t) throws Exception;
}
