package com.example.demo.framework.di.dynamicdatasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        // 從 ThreadLocal 中取得當前設定的資料來源鍵
        return DataSourceContextHolder.getDataSourceKey();
    }
}
