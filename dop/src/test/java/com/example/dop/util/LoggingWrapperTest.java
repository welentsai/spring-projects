package com.example.dop.util;

import com.example.dop.domain.City;
import com.example.dop.domain.Population;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.dop.util.LoggingWrapper.withLoggingAndExceptionHandling;

public class LoggingWrapperTest {

    @Test
    void shouldHaveLog() throws JsonProcessingException {
        List<City> cityList = List.of(
                new City("Hsinchu", new Population(200)),
                new City("Taichung", new Population(500)),
                new City("Tainan", new Population(300))
        );

        String cityListJson = JsonUtils.toJson(cityList);
        var toJsonFnWithLoggingFn = LoggingWrapper.withLoggingAndExceptionHandlingSafe("JsonUtils.toJson", JsonUtils::toJson);
        var resp = toJsonFnWithLoggingFn.apply(cityList);
        System.out.println(cityListJson);
        System.out.println(resp);
        Assertions.assertEquals(cityListJson, resp.get());
    }

    Integer addOne(Integer input) {
        return input + 1;
    }

    @Test
    void shouldHaveLog2() {
        var resp = withLoggingAndExceptionHandling("lambda", this::addOne).apply(1);
        System.out.println(resp);
    }
}
