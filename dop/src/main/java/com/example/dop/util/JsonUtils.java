package com.example.dop.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
        return objectMapper.readValue(json, clazz);
    }

    public static <T> T convertStringToList(String jsonArrayString, Class<T> elementType) throws JsonProcessingException {
        return objectMapper.readValue(jsonArrayString, objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));

    }


}
