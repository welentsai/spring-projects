package com.example.dop.controller;

import com.example.dop.AbstractContainerBase;
import com.example.dop.model.CityEntity;
import com.example.dop.repository.CityRepository;
import com.example.dop.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class CityControllerIntegrationTest extends AbstractContainerBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CityRepository cityRepository;

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void cleanup() {
        cityRepository.deleteAll();
    }

    @Test
    void testGetCity() throws JsonProcessingException {
        CityEntity newYork = new CityEntity("New York", "New York");
        var result = cityRepository.save(newYork);

        ResponseEntity<List<CityEntity>> response = restTemplate.exchange(
                "/api/v1/cities",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
        });

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertNotNull(response.getBody());
        Assertions.assertEquals(1, response.getBody().size());
        Assertions.assertEquals(JsonUtils.toJson(List.of(newYork)), JsonUtils.toJson(response.getBody()));
    }
}
