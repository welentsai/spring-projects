package com.example.demo.adapter.out.gateway;

import com.example.demo.usecase.ports.out.gateway.InlineRoutingGateway;
import com.example.demo.usecase.ports.out.gateway.InlineRoutingInput;
import com.example.demo.usecase.ports.out.gateway.InlineRoutingOutput;
import java.util.Random;

public class InlineRoutingGatewayImpl implements InlineRoutingGateway {
    private final Random random = new Random();

    @Override
    public InlineRoutingOutput execute(InlineRoutingInput input) {
        // 在真實場景中，這裡會有呼叫外部 API 的邏輯
        boolean usePrimary = random.nextBoolean();
        System.out.println(
                "External service decision: Use "
                        + (usePrimary ? "PRIMARY" : "SECONDARY")
                        + " data source.");
        return new InlineRoutingOutput(usePrimary ? "PRIMARY" : "SECONDARY");
    }
}
