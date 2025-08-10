package com.example.demo.framework.di;

import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.impl.FindCitiesUseCaseImpl;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public FindCitiesUseCase findCitiesUseCase(CitiesQueryRepository citiesQueryRepository) {
        return new FindCitiesUseCaseImpl(citiesQueryRepository);
    }
}
