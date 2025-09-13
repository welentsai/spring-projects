package com.example.dop.util;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "employee")
public record Employee(
        @JacksonXmlProperty(isAttribute = true)
        String id,
        String name,
        String department,
        double salary,
        String position,
        String manager) {
}
