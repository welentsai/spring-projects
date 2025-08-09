package com.example.demo.adapter.in.controller;

import com.example.demo.domain.model.City;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesOutput;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CitiesController.class)
public class CitiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindCitiesUseCase findCitiesUseCase;

    @Test
    public void testFindCities_should_return_city_list() throws Exception {
        List<City> cities = List.of(
                new City("1", "A", "TW"),
                new City("2", "B", "USA")
        );

        FindCitiesInput input = new FindCitiesInput();
        FindCitiesOutput output = new FindCitiesOutput(cities);

        when(findCitiesUseCase.execute(any())).thenReturn(output);

        MvcResult result = mockMvc.perform(get("/api/v1/cities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        String expectedResp = "{\"cities\":[{\"id\":\"1\",\"name\":\"A\",\"country\":\"TW\"},{\"id\":\"2\",\"name\":\"B\",\"country\":\"USA\"}]}";

        Assertions.assertEquals(expectedResp, responseBody);
    }


}
