package com.example.api.util;

import com.example.api.model.Product;

import jakarta.json.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.List;

/**
 * Converts between Product objects and JSON strings using the
 * Jakarta JSON Processing API (jsonp-2.1 feature in Liberty).
 * No third-party library required — Liberty provides the implementation.
 */
public class JsonUtil {

    private JsonUtil() {}

    // ── Serialize ─────────────────────────────────────────────────────────────

    /** Serializes a single Product to a JSON object string. */
    public static String toJson(Product p) {
        JsonObject obj = Json.createObjectBuilder()
            .add("id",    p.getId())
            .add("name",  p.getName())
            .add("price", p.getPrice())
            .add("stock", p.getStock())
            .build();
        return writeToString(obj);
    }

    /** Serializes a list of Products to a JSON array string. */
    public static String toJson(List<Product> products) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Product p : products) {
            builder.add(Json.createObjectBuilder()
                .add("id",    p.getId())
                .add("name",  p.getName())
                .add("price", p.getPrice())
                .add("stock", p.getStock())
            );
        }
        return writeToString(builder.build());
    }

    // ── Deserialize ───────────────────────────────────────────────────────────

    /**
     * Parses a JSON object string from a request body into a Product.
     * id is intentionally ignored — the path parameter is authoritative for
     * updates; for creates the DB generates the id.
     */
    public static Product fromJson(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            Product p = new Product();
            p.setName(obj.getString("name"));
            p.setPrice(readPrice(obj));
            p.setStock(obj.getInt("stock"));
            return p;
        }
    }

    /**
     * Reads "price" as a BigDecimal whether the caller sent it as a JSON number
     * (e.g. {"price":15.00}) or a JSON string (e.g. {"price":"15.00"}). Both are
     * valid producers in practice — curl examples tend to send raw numbers, while
     * products-ui sends quoted strings to avoid locale/formatting surprises.
     */
    private static BigDecimal readPrice(JsonObject obj) {
        JsonValue value = obj.get("price");
        if (value.getValueType() == JsonValue.ValueType.STRING) {
            return new BigDecimal(((JsonString) value).getString());
        }
        return ((JsonNumber) value).bigDecimalValue();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Wraps a JSON error message as { "error": "..." }. */
    public static String errorJson(String message) {
        return Json.createObjectBuilder()
            .add("error", message)
            .build()
            .toString();
    }

    private static String writeToString(JsonStructure structure) {
        StringWriter sw = new StringWriter();
        try (JsonWriter writer = Json.createWriter(sw)) {
            writer.write(structure);
        }
        return sw.toString();
    }
}
