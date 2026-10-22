package com.dantsu.thermalprinter.model;

public class Product {
    private long id;
    private String name;
    private String category;
    private double price;
    private double cost;
    private int stock;
    private int minThreshold;

    public Product() {
        this.minThreshold = 5;
    }

    public Product(long id, String name, String category, double price, double cost, int stock, int minThreshold) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.cost = cost;
        this.stock = stock;
        this.minThreshold = minThreshold;
    }

    public Product(String name, String category, double price, double cost, int stock, int minThreshold) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.cost = cost;
        this.stock = stock;
        this.minThreshold = minThreshold;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return (category == null || category.trim().isEmpty()) ? "General" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getMinThreshold() {
        return minThreshold <= 0 ? 5 : minThreshold;
    }

    public void setMinThreshold(int minThreshold) {
        this.minThreshold = minThreshold;
    }

    public boolean isOutOfStock() {
        return stock <= 0;
    }

    public boolean isLowStock() {
        return stock > 0 && stock <= getMinThreshold();
    }
}
