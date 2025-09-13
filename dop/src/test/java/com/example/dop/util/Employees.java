package com.example.dop.util;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "employees")
public record Employees(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "employee")
        List<Employee> employee
) {
}
