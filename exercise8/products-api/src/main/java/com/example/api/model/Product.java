package com.example.api.model;

import java.math.BigDecimal;

/**
 * Plain Java bean representing one row in the products table.
 * id, name, price, stock map directly to the DB columns.
 */
public class Product {

    private int id;
    private String name;
    private BigDecimal price;
    private int stock;

    public Product() {}

    public Product(int id, String name, BigDecimal price, int stock) {
        this.id    = id;
        this.name  = name;
        this.price = price;
        this.stock = stock;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public int getId()           { return id; }
    public String getName()      { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStock()        { return stock; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(int id)              { this.id    = id; }
    public void setName(String name)       { this.name  = name; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setStock(int stock)        { this.stock = stock; }
}
