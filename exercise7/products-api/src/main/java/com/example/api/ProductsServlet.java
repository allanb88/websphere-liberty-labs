package com.example.api;

import com.example.api.db.ProductDAO;
import com.example.api.model.Product;
import com.example.api.util.JsonUtil;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API servlet for the products resource.
 *
 * Routes:
 *   GET    /api/products        → list all products
 *   GET    /api/products/{id}   → get one product
 *   POST   /api/products        → create a product  (body: JSON)
 *   PUT    /api/products/{id}   → update a product  (body: JSON)
 *   DELETE /api/products/{id}   → delete a product
 *
 * The DataSource is injected by Liberty via @Resource using the JNDI name
 * declared in server.xml. Liberty's jdbc-4.2 + jndi-1.0 features handle
 * connection pooling; the servlet never opens connections directly.
 */
@WebServlet("/products/*")
public class ProductsServlet extends HttpServlet {

    @Resource(lookup = "java:comp/env/jdbc/products")
    private DataSource dataSource;

    private ProductDAO dao;

    @Override
    public void init() throws ServletException {
        dao = new ProductDAO(dataSource);
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo(); // null  → /products
                                             // "/3"  → /products/3

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // List all
                List<Product> products = dao.findAll();
                writeJson(resp, HttpServletResponse.SC_OK, JsonUtil.toJson(products));

            } else {
                // Get by id
                int id = parseId(pathInfo, resp);
                if (id < 0) return;                  // parseId already sent 400

                Product product = dao.findById(id);
                if (product == null) {
                    writeJson(resp, HttpServletResponse.SC_NOT_FOUND,
                              JsonUtil.errorJson("Product not found: " + id));
                } else {
                    writeJson(resp, HttpServletResponse.SC_OK, JsonUtil.toJson(product));
                }
            }
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      JsonUtil.errorJson(e.getMessage()));
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String body = readBody(req);
        try {
            Product created = dao.create(JsonUtil.fromJson(body));
            writeJson(resp, HttpServletResponse.SC_CREATED, JsonUtil.toJson(created));
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      JsonUtil.errorJson(e.getMessage()));
        }
    }

    // ── PUT ───────────────────────────────────────────────────────────────────

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int id = parseId(req.getPathInfo(), resp);
        if (id < 0) return;

        String body = readBody(req);
        try {
            Product updated = dao.update(id, JsonUtil.fromJson(body));
            if (updated == null) {
                writeJson(resp, HttpServletResponse.SC_NOT_FOUND,
                          JsonUtil.errorJson("Product not found: " + id));
            } else {
                writeJson(resp, HttpServletResponse.SC_OK, JsonUtil.toJson(updated));
            }
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      JsonUtil.errorJson(e.getMessage()));
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int id = parseId(req.getPathInfo(), resp);
        if (id < 0) return;

        try {
            boolean deleted = dao.delete(id);
            if (!deleted) {
                writeJson(resp, HttpServletResponse.SC_NOT_FOUND,
                          JsonUtil.errorJson("Product not found: " + id));
            } else {
                resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (SQLException e) {
            writeJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                      JsonUtil.errorJson(e.getMessage()));
        }
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    /**
     * Writes a JSON response with the given HTTP status.
     * Always sets Content-Type: application/json; charset=UTF-8.
     */
    private void writeJson(HttpServletResponse resp, int status, String json)
            throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(json);
    }

    /**
     * Parses the numeric id from a path segment like "/3".
     * Writes a 400 response and returns -1 if the segment is missing or not numeric.
     */
    private int parseId(String pathInfo, HttpServletResponse resp) throws IOException {
        if (pathInfo == null || pathInfo.equals("/")) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                      JsonUtil.errorJson("Product id is required"));
            return -1;
        }
        try {
            return Integer.parseInt(pathInfo.substring(1)); // strip leading "/"
        } catch (NumberFormatException e) {
            writeJson(resp, HttpServletResponse.SC_BAD_REQUEST,
                      JsonUtil.errorJson("Invalid product id: " + pathInfo.substring(1)));
            return -1;
        }
    }

    /** Reads the full request body as a UTF-8 string. */
    private String readBody(HttpServletRequest req) throws IOException {
        try (BufferedReader reader = req.getReader()) {
            return reader.lines().collect(Collectors.joining());
        }
    }
}
