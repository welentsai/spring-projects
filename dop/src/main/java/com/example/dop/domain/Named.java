package com.example.dop.domain;

/**
 * sealed keyword - the sealed type has to know all its extensions as types
 * it means sealed interface just like a "sum type"
 */
public sealed interface Named permits City, Department, Region {
    String name();
}
