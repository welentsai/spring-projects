package com.example.dop;

import com.example.dop.domain.City;
import com.example.dop.domain.Population;
import com.example.dop.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ObjectMapperTest {
    @Test
    public void testObjectToJson() throws JsonProcessingException {
        String expectedString = "{\"name\":\"Hsinchu\",\"population\":{\"amount\":200}}";
        City city = new City("Hsinchu", new Population(200));
        Assertions.assertEquals(expectedString, JsonUtils.toJson(city));
    }

    @Test
    public void testJsonToObject() throws JsonProcessingException {
        City expectedCity = new City("Hsinchu", new Population(200));
        String cityAsString = "{\"name\":\"Hsinchu\",\"population\":{\"amount\":200}}";
        Assertions.assertEquals(expectedCity, JsonUtils.fromJson(cityAsString, City.class));
    }

    @Test
    public void testObjectsToJson() throws JsonProcessingException {
        List<City> cityList = List.of(
                new City("Hsinchu", new Population(200)),
                new City("Taichung", new Population(500)),
                new City("Tainan", new Population(300))
        );

        String result = JsonUtils.toJson(cityList);
        System.out.println(result);
    }

    @Test
    public void testJsonToList() throws JsonProcessingException {
        List<City> cityList = List.of(
                new City("Hsinchu", new Population(200)),
                new City("Taichung", new Population(500)),
                new City("Tainan", new Population(300))
        );
        String cityListJson = JsonUtils.toJson(cityList);
        Assertions.assertEquals(cityList, JsonUtils.convertStringToList(cityListJson, City.class));
    }


}
