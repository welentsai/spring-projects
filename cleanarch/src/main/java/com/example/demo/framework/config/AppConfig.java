package com.example.demo.framework.config;

import com.example.demo.adapter.out.gateway.InlineRoutingGatewayImpl;
import com.example.demo.adapter.out.gateway.PutLogGatewayImpl;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.impl.FindCitiesUseCaseImpl;
import com.example.demo.usecase.ports.out.gateway.InlineRoutingGateway;
import com.example.demo.usecase.ports.out.gateway.PutLogGateway;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public InlineRoutingGateway inlineRoutingGateway() {
        return new InlineRoutingGatewayImpl();
    }

    @Bean
    public FindCitiesUseCase findCitiesUseCase(
            CitiesQueryRepository citiesQueryRepository,
            InlineRoutingGateway inlineRoutingGateway,
            PutLogGateway putLogGateway) {
        return new FindCitiesUseCaseImpl(
                citiesQueryRepository, inlineRoutingGateway, putLogGateway);
    }

    @Bean
    public PutLogGateway putLogGateway() {
        return new PutLogGatewayImpl();
    }
}
