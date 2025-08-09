package com.example.dop.config;

import com.example.dop.util.RequestResponseLoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

//    @Autowired
//    RequestResponseLoggingInterceptor loggingInterceptor;

    private final RequestResponseLoggingInterceptor loggingInterceptor;

    // Constructor injection - Spring will automatically inject the bean from InterceptorConfig
    public WebConfig(@Qualifier("requestResponseLoggingInterceptor") RequestResponseLoggingInterceptor loggingInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**") // Only log API endpoints
                .excludePathPatterns("/actuator/**"); // Exclude actuator endpoints
    }
}
