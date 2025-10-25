package com.example.demo.usecase.ports.in.impl;

import com.example.demo.framework.di.dynamicdatasource.DataSourceContextHolder;
import com.example.demo.framework.di.dynamicdatasource.DataSourceKey;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.dto.CityDto;
import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import com.example.demo.usecase.ports.out.gateway.*;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FindCitiesUseCaseImpl implements FindCitiesUseCase {
    private static final Logger logger = LoggerFactory.getLogger(FindCitiesUseCaseImpl.class);

    private final CitiesQueryRepository citiesQueryRepository;

    private final InlineRoutingGateway inlineRoutingGateway;

    private final PutLogGateway putLogGateway;

    public FindCitiesUseCaseImpl(
            CitiesQueryRepository citiesQueryRepository,
            InlineRoutingGateway inlineRoutingGateway,
            PutLogGateway putLogGateway) {
        this.citiesQueryRepository = citiesQueryRepository;
        this.inlineRoutingGateway = inlineRoutingGateway;
        this.putLogGateway = putLogGateway;
    }

    @Override
    public FindCitiesResult execute(FindCitiesInput input) {
        try {
            // Get routing decision from external service
            InlineRoutingOutput output = inlineRoutingGateway.execute(new InlineRoutingInput());
            logger.info("Inline routing output is {}", output);
            putLogGateway.execute(new PutLogInput("Inline routing output is", output));

            // 在執行資料庫操作前，設定當前執行緒的資料來源
            if (output.datasourceKey().equalsIgnoreCase("PRIMARY")) {
                DataSourceContextHolder.setDataSourceKey(DataSourceKey.PRIMARY);
            } else {
                DataSourceContextHolder.setDataSourceKey(DataSourceKey.SECONDARY);
            }

            // 4. 執行業務邏輯 (JPA 操作)
            // Spring Data JPA 會透過我們的 DynamicRoutingDataSource 找到正確的資料來源

            // Set the data source context based on routing decision
            DataSourceKey dataSourceKey =
                    "PRIMARY".equals(output.datasourceKey())
                            ? DataSourceKey.PRIMARY
                            : DataSourceKey.SECONDARY;

            DataSourceContextHolder.setDataSourceKey(dataSourceKey);
            logger.info("Set data source context to: {}", dataSourceKey);
            putLogGateway.execute(new PutLogInput("et data source context to: ", output));

            try {
                // Use the repository to get all cities (will use the selected data source)
                List<CityJpaEntity> cityEntities = citiesQueryRepository.findAll();

                // Map JPA entities to DTOs
                List<CityDto> cityDtos =
                        cityEntities.stream()
                                .map(
                                        entity ->
                                                new CityDto(
                                                        entity.getId(),
                                                        entity.getName(),
                                                        entity.getCountry()))
                                .toList();

                return FindCitiesResult.success(cityDtos);
            } finally {
                // Always clear the data source context to prevent memory leaks
                DataSourceContextHolder.clearDataSourceKey();
                logger.debug("Cleared data source context");
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve cities", e);
            putLogGateway.execute(new PutLogInput("Failed to retrieve cities: ", e.getMessage()));
            return FindCitiesResult.failure(
                    "INTERNAL_ERROR", "Failed to retrieve cities: " + e.getMessage());
        }
    }
}
