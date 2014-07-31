package com.linkedin.dataholder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bbarkley
 */
public class DataHolder {
    public static ThreadLocal<DataHolder> INSTANCE = new ThreadLocal<DataHolder>();

    public static DataHolder getInstance() {
        return INSTANCE.get();
    }

    private volatile int count = 0;

    public volatile Map<String, Object> data = new HashMap<>();

    public DataHolder() {}

    private DataHolder(int count, Map<String, Object> data) {
        this.count = count;
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) { this.count = count; }
    public void incrementCount() {
        count++;
    }

    public DataHolder copy() {
        return new DataHolder(getCount(), getData());
    }

    @Override
    public String toString() {
        return "DataHolder: " + count;
    }
}
