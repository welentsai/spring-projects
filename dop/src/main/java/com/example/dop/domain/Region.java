package com.example.dop.domain;

import java.util.Objects;

public final class Region implements Named{
    private final String name;

    public Region(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
