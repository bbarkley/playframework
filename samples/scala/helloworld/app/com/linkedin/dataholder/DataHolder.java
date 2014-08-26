package com.linkedin.dataholder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bbarkley
 */
public class DataHolder {
    public static ThreadLocal<DataHolder> INSTANCE = new ThreadLocal<DataHolder>();

    public static DataHolder getInstance() {
        return INSTANCE.get();
    }

    private volatile int count = 0;

    public volatile Map<String, Object> data = new ConcurrentHashMap<>();

    public DataHolder() {}

    private DataHolder(int count, Map<String, Object> data) {
        this.count = count;
        this.data = data;
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    public void addData(Map<String, Object> data) {
        this.data.putAll(data);
    }

    public void addData(String key, Object value) {
        this.data.put(key, value);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) { this.count = count; }
    public void incrementCount() {
        count++;
    }

    public DataHolder copy() {
        return new DataHolder(getCount(), new HashMap<>(getData()));
    }

    @Override
    public String toString() {
        return "DataHolder: " + count + " and " + data + System.identityHashCode(this);
    }
}
