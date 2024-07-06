package com.example.dop.domain;

import com.example.dop.domain.business.BusinessCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class NamedTest {
    @Test
    public void testCity() {
        City newCity = new City("Hsinchu", new Population(20));
        Assertions.assertEquals("Hsinchu", newCity.name());
    }

    @Test
    public void testCreateCityFail() {
        Exception exception = Assertions.assertThrows(NullPointerException.class, () -> {
            new City(null, null);
        });

        Assertions.assertNull(exception.getMessage());
    }

    @Test
    public void testNamedCompare() {
        City qudon = new City("Qudon", new Population(20));

        List<Named> nameds = List.of(new Department("Moselle", List.of(qudon)), qudon, new Department("Herrault", List.of(qudon)));

        Assertions.assertEquals("Herrault", BusinessCode.min(nameds).toString());
    }

    @Test
    public void testFunctionGetName() {
        City qudon = new City("Qudon", new Population(20));
        String result = BusinessCode.getName().apply(qudon);
        System.out.println(result);
    }
}
