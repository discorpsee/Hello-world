package com.epam.settings;

import java.util.concurrent.TimeUnit;

public class ConfigurationBuilder {
    private PoolConfiguration config;

    private ConfigurationBuilder() {
    }

    private ConfigurationBuilder(PoolConfiguration config) {
        this.config = config;
    }

    public static ConfigurationBuilder start() {
        return new ConfigurationBuilder(new PoolConfiguration());
    }

    public ConfigurationBuilder url(String url) {
        config.setUrl(url);
        return this;
    }

    public ConfigurationBuilder driver(String driver) {
        config.setDriver(driver);
        return this;
    }

    public ConfigurationBuilder user(String user) {
        config.setUser(user);
        return this;
    }

    public ConfigurationBuilder password(String password) {
        config.setPassword(password);
        return this;
    }

    public ConfigurationBuilder minSize(int size) {
        config.setMinSize(size);
        return this;
    }

    public ConfigurationBuilder maxSize(int size) {
        config.setMaxSize(size);
        return this;
    }

    public ConfigurationBuilder maxWait(int maxWait) {
        config.setMaxWait(maxWait);
        return this;
    }

    public ConfigurationBuilder sweeper(boolean isSweeperEnabled) {
        config.setSweeperEnabled(isSweeperEnabled);
        return this;
    }

    public ConfigurationBuilder timeBetweenEvictionRuns(long timeBetweenEvictionRuns, TimeUnit timeUnit) {
        config.setTimeBetweenEvictionRunsMillis(timeUnit.toMillis(timeBetweenEvictionRuns));
        return this;
    }

    public ConfigurationBuilder forceClose(boolean forceClose) {
        config.setForceClose(forceClose);
        return this;
    }

    public ConfigurationBuilder returnOnClose(boolean returnOnClose) {
        config.setReturnOnClose(returnOnClose);
        return this;
    }

    public PoolConfiguration build() {
        return this.config;
    }
}
