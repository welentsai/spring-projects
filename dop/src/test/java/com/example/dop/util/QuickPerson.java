package com.example.dop.util;

// Simple record - works out of the box
public record QuickPerson(
        String id,
        String firstName,
        String lastName,
        Integer age,
        String birthDate,
        String email
) {
}