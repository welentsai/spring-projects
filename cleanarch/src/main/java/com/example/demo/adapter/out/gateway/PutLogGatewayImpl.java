package com.example.demo.adapter.out.gateway;

import com.example.demo.system.LoggingContext;
import com.example.demo.usecase.ports.out.gateway.PutLogGateway;
import com.example.demo.usecase.ports.out.gateway.PutLogInput;
import com.example.demo.usecase.ports.out.gateway.PutLogOutput;

public class PutLogGatewayImpl implements PutLogGateway {
    @Override
    public PutLogOutput execute(PutLogInput input) {
        return PutLogOutput.of(LoggingContext.put(input.key(), input.value()));
    }
}
