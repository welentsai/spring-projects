package com.example.demo.framework.di;

import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.impl.FindCitiesUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public FindCitiesUseCase findCitiesUseCase() {
        return new FindCitiesUseCaseImpl();
    }
}
