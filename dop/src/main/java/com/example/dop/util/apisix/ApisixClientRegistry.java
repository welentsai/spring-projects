package com.example.dop.util.apisix;

import com.example.dop.util.RetryableRestClient;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ApisixClientRegistry {

    private final Map<String, RetryableRestClient> clients;

    public ApisixClientRegistry(Map<String, RetryableRestClient> clients) {
        this.clients = Map.copyOf(clients);
    }

    public RetryableRestClient getClient(String gatewayName) {
        return Optional.ofNullable(clients.get(gatewayName))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown APISIX gateway: '" + gatewayName + "'. Available: "
                                + clients.keySet()));
    }

    public Set<String> gatewayNames() {
        return clients.keySet();
    }
}
