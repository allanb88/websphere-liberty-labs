package com.example.ui;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backing servlet for the products UI. Every request flows through here first:
 * GET renders the table via products.jsp; POST handles create/update/delete via a
 * hidden _method field, since HTML forms only support GET/POST natively. All data
 * access goes through ApiClient — this servlet never talks to the database.
 *
 * After every mutation it redirects back to GET /ui/products (Post-Redirect-Get),
 * so a browser refresh never resubmits a form.
 */
@WebServlet("/products/*")
public class ProductsController extends HttpServlet {

    private ApiClient api;

    @Override
    public void init() throws ServletException {
        api = new ApiClient("http://products-api:9081/api/products");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            String json = api.get("");
            req.setAttribute("products", parseList(json));
        } catch (IOException e) {
            req.setAttribute("errorMessage", "Could not reach products-api: " + e.getMessage());
            req.setAttribute("products", List.of());
        }
        req.getRequestDispatcher("/products.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String method = req.getParameter("_method");
        if (method == null) {
            method = "POST";
        }

        try {
            if ("PUT".equalsIgnoreCase(method)) {
                String id = req.getParameter("id");
                api.put("/" + id, toJson(req));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                String id = req.getParameter("id");
                api.delete("/" + id);
            } else {
                api.post(toJson(req));
            }
        } catch (IOException e) {
            // Swallow and redirect anyway — the reloaded list reflects the API's
            // actual state either way. A production version would carry this as
            // a flash-scoped error message across the redirect.
        }

        resp.sendRedirect(req.getContextPath() + "/products");
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    /**
     * Builds the JSON body sent to products-api from form fields. price is sent
     * as a JSON string (not a bare number) to sidestep locale/decimal-formatting
     * ambiguity in HTML number inputs — JsonUtil.fromJson on the API side accepts
     * both forms.
     */
    private String toJson(HttpServletRequest req) {
        return Json.createObjectBuilder()
                .add("name", req.getParameter("name"))
                .add("price", req.getParameter("price"))
                .add("stock", Integer.parseInt(req.getParameter("stock")))
                .build()
                .toString();
    }

    private List<Map<String, String>> parseList(String json) {
        List<Map<String, String>> result = new ArrayList<>();
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonArray array = reader.readArray();
            for (JsonValue value : array) {
                JsonObject obj = value.asJsonObject();
                Map<String, String> row = new LinkedHashMap<>();
                row.put("id", String.valueOf(obj.getInt("id")));
                row.put("name", obj.getString("name"));
                row.put("price", obj.get("price").toString());
                row.put("stock", String.valueOf(obj.getInt("stock")));
                result.add(row);
            }
        }
        return result;
    }
}
