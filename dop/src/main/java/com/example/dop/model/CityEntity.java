package com.example.dop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = "cities")
public class CityEntity {
    @Id
    private String id;

    private String name;
    private String country;

    public CityEntity(String name, String country) {
        this.name = name;
        this.country = country;
    }

    public String getId() {return this.id;}
    public String getName() {return this.name;}
    public String getCountry() {return this.country;}
}
