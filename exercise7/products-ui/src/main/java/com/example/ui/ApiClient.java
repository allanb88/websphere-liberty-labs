package com.example.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Thin HTTP client for the products-api REST service, built on java.net.HttpURLConnection
 * only — no third-party HTTP library. Every CRUD action from the UI flows through here;
 * the UI never talks to the database directly.
 *
 * baseUrl uses the products-api container's name on the shared Docker network
 * (products-net), not localhost — from inside the products-ui container, "localhost"
 * would mean "this container", not the API container.
 */
public class ApiClient {

    private final String baseUrl;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String get(String path) throws IOException {
        return send("GET", path, null);
    }

    public String post(String jsonBody) throws IOException {
        return send("POST", "", jsonBody);
    }

    public String put(String path, String jsonBody) throws IOException {
        return send("PUT", path, jsonBody);
    }

    public void delete(String path) throws IOException {
        send("DELETE", path, null);
    }

    private String send(String method, String path, String jsonBody) throws IOException {
        URI uri = URI.create(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (jsonBody != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
            }

            int status = conn.getResponseCode();
            InputStream body = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String responseText = readAll(body);

            if (status >= 400) {
                throw new IOException("products-api returned HTTP " + status + ": " + responseText);
            }
            return responseText;
        } finally {
            conn.disconnect();
        }
    }

    private String readAll(InputStream body) throws IOException {
        if (body == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
