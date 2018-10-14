package com.epam.pool.exception;

public class WrongConnectionsCount extends Exception {
    public WrongConnectionsCount() {
    }

    public WrongConnectionsCount(String message) {
        super(message);
    }
}
