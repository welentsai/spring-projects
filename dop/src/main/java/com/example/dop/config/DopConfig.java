package com.example.dop.config;

import com.example.dop.repository.CityEntityRepository;
import com.example.dop.service.CityService;
import com.example.dop.service.CityServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DopConfig {

    @Bean
    public CityService getCityService(CityEntityRepository cityRepository) {
        return new CityServiceImpl(cityRepository);
    }
}
