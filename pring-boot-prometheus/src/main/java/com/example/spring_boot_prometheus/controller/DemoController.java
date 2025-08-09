package com.example.spring_boot_prometheus.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Time;

@RestController
public class DemoController {

    private final Counter requestCounter;

    public DemoController(MeterRegistry registry) {
        this.requestCounter = Counter.builder("demo_requests_total")
                .description("Total number of requests")
                .register(registry);
    }

    @GetMapping("/hello")
    public String hello() {
        System.out.println("hello Api called " + System.currentTimeMillis());
        requestCounter.increment();
        return "Hello, Prometheus!";
    }
}
