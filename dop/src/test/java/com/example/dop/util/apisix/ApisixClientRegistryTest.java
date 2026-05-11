package com.example.dop.util.apisix;

import com.example.dop.util.RetryableRestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApisixClientRegistryTest {

    @Mock RetryableRestClient paymentClient;
    @Mock RetryableRestClient inventoryClient;

    private ApisixClientRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ApisixClientRegistry(Map.of(
                "payment", paymentClient,
                "inventory", inventoryClient));
    }

    @Test
    void getClient_returnsCorrectClientByName() {
        assertSame(paymentClient, registry.getClient("payment"));
        assertSame(inventoryClient, registry.getClient("inventory"));
    }

    @Test
    void getClient_throwsForUnknownGateway() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getClient("unknown"));

        assertTrue(ex.getMessage().contains("'unknown'"));
        assertTrue(ex.getMessage().contains("Available"));
    }

    @Test
    void gatewayNames_returnsAllConfiguredNames() {
        assertEquals(Set.of("payment", "inventory"), registry.gatewayNames());
    }

    @Test
    void registry_isImmutable() {
        // Map.copyOf ensures the internal map cannot be modified externally
        Map<String, RetryableRestClient> mutableMap = new java.util.HashMap<>();
        mutableMap.put("payment", paymentClient);

        ApisixClientRegistry r = new ApisixClientRegistry(mutableMap);
        mutableMap.put("injected", inventoryClient); // mutate source map after construction

        // Registry should not reflect the post-construction mutation
        assertThrows(IllegalArgumentException.class, () -> r.getClient("injected"));
        assertEquals(Set.of("payment"), r.gatewayNames());
    }
}
