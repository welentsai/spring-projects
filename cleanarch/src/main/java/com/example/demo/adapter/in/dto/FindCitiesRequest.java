package com.example.demo.adapter.in.dto;

import java.util.Optional;

public record FindCitiesRequest() {

    public Optional<FindCitiesRequest> validate() {
        return Optional.of(this);
    }
}
