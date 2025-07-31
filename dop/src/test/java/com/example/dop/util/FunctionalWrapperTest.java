package com.example.dop.util;

import com.example.dop.domain.City;
import com.example.dop.domain.Population;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FunctionalWrapperTest {

    @Test
    void shouldHaveLog() throws JsonProcessingException {
        List<City> cityList = List.of(
                new City("Hsinchu", new Population(200)),
                new City("Taichung", new Population(500)),
                new City("Tainan", new Population(300))
        );

        String cityListJson = JsonUtils.toJson(cityList);
        var toJsonFnWithLoggingFn = FunctionalWrapper.withLoggingAndExceptionHandlingSafe("JsonUtils.toJson", JsonUtils::toJson);
        var resp = toJsonFnWithLoggingFn.apply(cityList);
        System.out.println(cityListJson);
        System.out.println(resp);
        Assertions.assertEquals(cityListJson, resp.get());
    }
}
