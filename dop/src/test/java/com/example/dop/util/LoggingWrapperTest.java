package com.example.dop.util;

import com.example.dop.domain.City;
import com.example.dop.domain.Population;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.dop.util.LoggingWrapper.tryWithLoggingAndCustomHandler;
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
        var result = input + 1;

        // for test only
        if (result == 10) {
            throw new IllegalArgumentException("Error");
        }
        return result;
    }

    @Test
    void shouldHaveLog2() {
        var resp = withLoggingAndExceptionHandling("lambda", this::addOne).apply(1);
        System.out.println(resp);
    }

    @Test
    void shouldHaveLog3() {
        var addOneWithLoggingFn = withLoggingAndExceptionHandling("addOne", () -> addOne(1));
        var resp = addOneWithLoggingFn.get();
        Assertions.assertEquals(2, resp);
    }

    @Test
    void shouldHaveLogWithCustomExceptionHandlingSupplier() {
        var addOneWithLoggingFn = tryWithLoggingAndCustomHandler("addOne",
                () -> addOne(1),
                error -> {
                    System.out.println(error);
                    throw new RuntimeException("test");
                });

        var resp = addOneWithLoggingFn.get();
        System.out.println(resp);
    }

    @Test
    void shouldHaveLogWithCustomExceptionHandlingFunction() {
        var addOneWithLoggingFn = tryWithLoggingAndCustomHandler(
                "addOne",
                this::addOne,
                error -> {
                    System.out.println(error);
                    throw new RuntimeException("test");
                });

        var resp = addOneWithLoggingFn.apply(1);
        System.out.println(resp);
    }

    @Test
    void shouldHaveLogWithCustomExceptionHandlingException() {
        var addOneWithLoggingFn = LoggingWrapper.tryWithLoggingAndCustomHandler("addOne",
                this::addOne,
                error -> {
                    System.out.println(error);
                    return 0;
                });

        var resp = addOneWithLoggingFn.apply(9);
        System.out.println("resp:" + resp);
    }

    @Test
    void shouldHaveLogWithExceptionHandling() {
        var externalCall = tryWithLoggingAndCustomHandler("HTTP Get",
                OkHttpClientUtils::get,
                error -> {
                    System.out.println("error:" + error);
                    throw new RuntimeException("test");
                });

        var resp = externalCall.apply("https://hn.algolia.com/api/v1/search?query=react");
        System.out.println(resp);
    }
}
