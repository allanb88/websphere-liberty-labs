package com.example.api.util;

import com.example.api.model.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilTest {

    @Test
    void toJsonSerializesAllFieldsOfASingleProduct() {
        Product p = new Product(1, "Wireless Keyboard", new BigDecimal("49.99"), 120);

        String json = JsonUtil.toJson(p);

        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"Wireless Keyboard\""));
        assertTrue(json.contains("\"price\":49.99"));
        assertTrue(json.contains("\"stock\":120"));
    }

    @Test
    void toJsonSerializesAListAsAJsonArray() {
        List<Product> products = List.of(
                new Product(1, "Keyboard", new BigDecimal("49.99"), 120),
                new Product(2, "Mouse", new BigDecimal("59.00"), 60)
        );

        String json = JsonUtil.toJson(products);

        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("Keyboard"));
        assertTrue(json.contains("Mouse"));
    }

    // Regression test: products-ui sends "price" as a JSON *string* (to avoid
    // locale/decimal formatting ambiguity from an HTML number input), while a
    // plain curl call tends to send it as a JSON *number*. fromJson used to
    // assume it was always a string and threw ClassCastException on a number.
    @Test
    void fromJsonAcceptsPriceAsJsonNumber() {
        Product p = JsonUtil.fromJson("{\"name\":\"Monitor Stand\",\"price\":29.99,\"stock\":40}");

        assertEquals("Monitor Stand", p.getName());
        assertEquals(0, new BigDecimal("29.99").compareTo(p.getPrice()));
        assertEquals(40, p.getStock());
    }

    @Test
    void fromJsonAcceptsPriceAsJsonString() {
        Product p = JsonUtil.fromJson("{\"name\":\"Monitor Stand\",\"price\":\"29.99\",\"stock\":40}");

        assertEquals("Monitor Stand", p.getName());
        assertEquals(0, new BigDecimal("29.99").compareTo(p.getPrice()));
        assertEquals(40, p.getStock());
    }

    @Test
    void fromJsonIgnoresAnyIdFieldInTheBody() {
        // id is intentionally not read from the body — the path parameter (update)
        // or the DB-generated value (create) is authoritative, never client input.
        Product p = JsonUtil.fromJson("{\"id\":999,\"name\":\"Mouse\",\"price\":10,\"stock\":5}");

        assertEquals(0, p.getId());
    }

    @Test
    void errorJsonWrapsTheMessageUnderAnErrorKey() {
        String json = JsonUtil.errorJson("stock cannot be negative");

        assertEquals("{\"error\":\"stock cannot be negative\"}", json);
    }
}
