package com.example.dop.controller;

import com.example.dop.model.CityEntity;
import com.example.dop.service.CityServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CityController.class)
public class CityEntityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CityServiceImpl cityService;

    @Test
    void getAllCities() throws Exception {
        var expectedCities = List.of(
                new CityEntity("Taichung", "Taiwan"),
                new CityEntity("Tainan", "Taiwan")
        );

        when(cityService.getAllCities()).thenReturn(expectedCities);

        mockMvc.perform(get("/api/v1/cities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

    }

}
