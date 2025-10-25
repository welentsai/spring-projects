package com.example.demo.usecase.ports.out.gateway;

import java.util.Map;

public interface PutLogGateway {
    PutLogOutput execute(PutLogInput input);
}
