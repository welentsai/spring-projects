package com.example.dop.config;

import com.example.dop.repository.CityRepository;
import com.example.dop.service.CityService;
import com.example.dop.service.CityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DopConfig {

    @Autowired
    private CityRepository cityRepository;

    @Bean
    public CityService getCityService() {
        return new CityServiceImpl(cityRepository);
    }
}
