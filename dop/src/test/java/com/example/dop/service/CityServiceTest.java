package com.example.dop.service;

import com.example.dop.model.CityEntity;
import com.example.dop.repository.CityRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;

public class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    private CityService cityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cityService = new CityService(cityRepository);
    }

    @Test
    public void findAll() {
        CityEntity newYork = new CityEntity("New York", "New York");
        CityEntity london = new CityEntity("London", "London");
        var expectedCities = List.of(newYork, london);
        when(cityRepository.findAll()).thenReturn(expectedCities);

        List<CityEntity> actualCities = cityService.getAllCities();
        Assertions.assertEquals(expectedCities, actualCities);
        verify(cityRepository, times(1)).findAll();
    }
}
