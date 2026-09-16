package com.example.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductsControllerTest {

    private final ProductsController controller = new ProductsController();

    @Test
    void parseListTurnsAJsonArrayIntoARowPerProduct() {
        String json = "["
                + "{\"id\":1,\"name\":\"Wireless Keyboard\",\"price\":49.99,\"stock\":120},"
                + "{\"id\":2,\"name\":\"USB-C Hub\",\"price\":34.95,\"stock\":85}"
                + "]";

        List<Map<String, String>> rows = controller.parseList(json);

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).get("id"));
        assertEquals("Wireless Keyboard", rows.get(0).get("name"));
        assertEquals("120", rows.get(0).get("stock"));
        assertEquals("USB-C Hub", rows.get(1).get("name"));
    }

    @Test
    void parseListReturnsAnEmptyListForAnEmptyJsonArray() {
        List<Map<String, String>> rows = controller.parseList("[]");

        assertTrue(rows.isEmpty());
    }
}
