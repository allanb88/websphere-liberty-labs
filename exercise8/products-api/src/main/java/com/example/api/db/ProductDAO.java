package com.example.api.db;

import com.example.api.model.Product;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * All JDBC operations against the products table.
 * Receives a DataSource provisioned by Liberty (via JNDI) — never opens
 * its own connection directly, so the container manages the pool.
 */
public class ProductDAO {

    private final DataSource dataSource;

    public ProductDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ── READ ─────────────────────────────────────────────────────────────────

    /** Returns every row in the products table, ordered by id. */
    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT id, name, price, stock FROM products ORDER BY id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Returns the product with the given id, or null if not found. */
    public Product findById(int id) throws SQLException {
        String sql = "SELECT id, name, price, stock FROM products WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    /** Inserts a new product and returns the full row including the generated id. */
    public Product create(Product p) throws SQLException {
        String sql = "INSERT INTO products (name, price, stock) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setBigDecimal(2, p.getPrice());
            ps.setInt(3, p.getStock());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                p.setId(rs.getInt("id"));
            }
        }
        return p;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    /**
     * Updates name, price, and stock for the product with the given id.
     * Returns the updated product, or null if no row with that id exists.
     */
    public Product update(int id, Product p) throws SQLException {
        String sql = "UPDATE products SET name = ?, price = ?, stock = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setBigDecimal(2, p.getPrice());
            ps.setInt(3, p.getStock());
            ps.setInt(4, id);
            int affected = ps.executeUpdate();
            if (affected == 0) return null;
        }
        p.setId(id);
        return p;
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    /**
     * Deletes the product with the given id.
     * Returns true if a row was deleted, false if the id was not found.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getInt("stock")
        );
    }
}
