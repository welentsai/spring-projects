package com.example.dop.util;

import java.util.List;

public record Order(String id, String customerId, String orderDate, String total, String status, List<Item> items) {
    public record Item(String productId, String quantity, String unitPrice) {
    }
}
