package com.example.dop.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class RetryableRestClientFactory {

    private final List<Map.Entry<String, RetryableRestClient>> sortedPrefixes;
    private final RetryableRestClient defaultClient;

    public RetryableRestClientFactory(
            Map<String, RetryableRestClient> clientsByBaseUrl,
            RetryableRestClient defaultClient) {
        this.defaultClient = defaultClient;
        // longest baseUrl first — most-specific prefix wins on overlap
        this.sortedPrefixes = clientsByBaseUrl.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getKey().length()))
                .toList();
    }

    public RetryableRestClient resolve(String endpoint) {
        return sortedPrefixes.stream()
                .filter(e -> endpoint.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaultClient);
    }
}
