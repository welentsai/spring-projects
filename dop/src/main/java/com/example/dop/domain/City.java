package com.example.dop.domain;

import java.util.Objects;

public record City (String name, Population population)
        implements Named {

    // compact constructor
    public City {
        Objects.requireNonNull(name);
        Objects.requireNonNull(population);
    }
}
