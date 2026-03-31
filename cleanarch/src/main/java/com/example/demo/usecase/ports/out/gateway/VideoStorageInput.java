package com.example.demo.usecase.ports.out.gateway;

import com.example.demo.usecase.ports.in.Input;

public record VideoStorageInput(String bucketName) implements Input {}
