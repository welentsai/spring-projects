package com.example.demo.framework.di.dynamicdatasource;

import org.springframework.util.Assert;

public class DataSourceContextHolder {

    private static final ThreadLocal<DataSourceKey> contextHolder = new ThreadLocal<>();

    public static void setDataSourceKey(DataSourceKey key) {
        Assert.notNull(key, "DataSourceKey cannot be null");
        contextHolder.set(key);
    }

    public static DataSourceKey getDataSourceKey() {
        return contextHolder.get();
    }

    // clearDataSourceKey() 方法至關重要，必須在每次請求處理完畢後呼叫，以防止記憶體洩漏和執行緒複用時的狀態污染。
    public static void clearDataSourceKey() {
        contextHolder.remove();
    }
}