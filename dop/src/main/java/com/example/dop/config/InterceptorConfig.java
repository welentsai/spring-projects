package com.example.dop.config;

import com.example.dop.util.RequestResponseLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;

@Configuration
public class InterceptorConfig {

    @Bean("requestResponseLoggingInterceptor")
    public RequestResponseLoggingInterceptor requestResponseLoggingInterceptor() {
        return new RequestResponseLoggingInterceptor();
    }
}
