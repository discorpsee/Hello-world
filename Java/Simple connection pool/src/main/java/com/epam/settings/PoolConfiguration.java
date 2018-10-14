package com.epam.settings;

import java.io.Serializable;

public class PoolConfiguration implements Serializable {
    public static final int DEFAULT_MAX_SIZE = 100;
    private static final boolean DEFAULT_RETURN_ON_CLOSE = true;

    private String url;
    private String user;
    private String password;
    /**
     * Драйвер
     */
    private String driver;
    /**
     * The minimum number of established connections that should be kept in the pool at all times.
     */
    private int minSize;
    /**
     * The maximum number of connections that should be kept in the idle pool.
     */
    private int maxSize;
    /**
     * Уборщик
     */
    private boolean sweeperEnabled;
    /**
     * Время между вызовами сборщика
     */
    private long timeBetweenEvictionRunsMillis;
    /**
     * Максимальное время ожидания
     */
    private long maxWait;
    /**
     * true - закрываются занятыве соединения
     * false - занятые соединения закроются при возвращении домой
     */
    private boolean forceClose;
    /**
     * При закрытии соединения:
     * true - оно возвращается в пул
     * false - оно закрывается
     */
    private boolean returnOnClose = DEFAULT_RETURN_ON_CLOSE;

    public PoolConfiguration() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isSweeperEnabled() {
        return sweeperEnabled;
    }

    public void setSweeperEnabled(boolean sweeperEnabled) {
        this.sweeperEnabled = sweeperEnabled;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public boolean isForceClose() {
        return forceClose;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setForceClose(boolean forceClose) {
        this.forceClose = forceClose;
    }

    public void setReturnOnClose(boolean returnOnClose) {
        this.returnOnClose = returnOnClose;
    }

    public boolean isReturnOnClose() {
        return returnOnClose;
    }

    @Override
    public String toString() {
        return "PoolConfiguration{" +
                "url='" + url + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", driver='" + driver + '\'' +
                ", minSize=" + minSize +
                ", maxSize=" + maxSize +
                ", sweeperEnabled=" + sweeperEnabled +
                ", timeBetweenEvictionRunsMillis=" + timeBetweenEvictionRunsMillis +
                ", maxWait=" + maxWait +
                ", forceClose=" + forceClose +
                ", returnOnClose=" + returnOnClose +
                '}';
    }
}
