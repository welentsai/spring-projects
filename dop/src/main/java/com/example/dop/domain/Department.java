package com.example.dop.domain;

import java.util.List;
import java.util.Objects;

public record Department(String name, List<City> cities) implements Named{

    public Department {
        name = Objects.requireNonNull(name);
        cities = List.copyOf(cities);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
