package com.example.dop.config;

import com.example.dop.repository.CityRepository;
import com.example.dop.service.CityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.junit.jupiter.Container;

@Configuration
public class DopConfig {

    @Autowired
    private CityRepository cityRepository;

    @Bean
    public CityService getCityService() {
        return new CityService(cityRepository);
    }
}
