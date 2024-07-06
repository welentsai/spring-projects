package com.example.dop.domain.business;

import com.example.dop.domain.*;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class BusinessCode {

    public static String name(Named named) {

        // pattern matching and deconstructing
        return switch (named) {
            case null -> "null";
            case Department(String name, List<City> cities) -> name;
            case City(String name, Population population) when population.amount() > 15 -> name;
            case City(String name, Population population) -> {
                System.out.println("pattern matched !! 2");
                yield name;
            }
            case Region region -> region.name();
        };
    }

    // java Function as return type
    public static Function<Named, String> getName() {
        return (named) -> switch (named) {
            case Department(String name, List<City> cities) -> name;
            case City(String name, Population population) -> name;
            case Region region -> region.name();
        };
    }

    public static Named min(List<Named> nameds) {
        return nameds.stream().min(Comparator.comparing(BusinessCode::name)).orElseThrow();
    }
}
