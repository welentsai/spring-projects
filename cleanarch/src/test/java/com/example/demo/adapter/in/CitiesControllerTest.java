package com.example.demo.adapter.in;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.dto.CityDto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

@WebMvcTest(controllers = CitiesController.class)
public class CitiesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    FindCitiesUseCase findCitiesUseCase;

    @Test
    public void testFindCities_should_return_city_list() throws Exception {
        List<CityDto> cityDtos = List.of(new CityDto("1", "A", "TW"), new CityDto("2", "B", "USA"));

        FindCitiesResult successResult = FindCitiesResult.success(cityDtos);

        when(findCitiesUseCase.execute(any())).thenReturn(successResult);

        MvcResult result = mockMvc.perform(
                        get("/api/v1/cities").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        String expectedResp =
                "{\"cities\":[{\"id\":\"1\",\"name\":\"A\",\"country\":\"TW\"},{\"id\":\"2\",\"name\":\"B\",\"country\":\"USA\"}]}";

        Assertions.assertEquals(expectedResp, responseBody);
    }
}
