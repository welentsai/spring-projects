package com.example.demo.usecase.ports.out.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CityJpaEntity {

    @Id private String id;

    private String name;
    private String country;

    public CityJpaEntity() {}

    public CityJpaEntity(String id, String name, String country) {
        this.id = id;
        this.name = name;
        this.country = country;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
