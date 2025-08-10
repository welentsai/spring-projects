package com.example.demo.framework.di.dynamicdatasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.inline.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.inline.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create()
                .type(com.zaxxer.hikari.HikariDataSource.class)
                .build();
    }

    @Bean
    @Primary // 將動態資料來源設定為主 DataSource Bean
    public DataSource dynamicDataSource(DataSource primaryDataSource, DataSource secondaryDataSource) {
        DynamicRoutingDataSource routingDataSource = new DynamicRoutingDataSource();

        // 設定所有目標資料來源
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceKey.PRIMARY, primaryDataSource);
        targetDataSources.put(DataSourceKey.SECONDARY, secondaryDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);

        // 設定預設資料來源
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);

        return routingDataSource;
    }

    // 使用 @Primary 註解在 dynamicDataSource bean 上，可以確保 Spring 的自動配置（如 JpaTransactionManager）會預設使用這個路由資料來源，而不是某個具體的資料來源。
}
