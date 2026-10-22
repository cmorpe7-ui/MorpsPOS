package com.dantsu.thermalprinter.model;

public class SaleTransactionItem {
    private long id;
    private long saleId;
    private String productName;
    private double unitPrice;
    private int quantity;
    private double subtotal;

    public SaleTransactionItem() {}

    public SaleTransactionItem(long id, long saleId, String productName, double unitPrice, int quantity, double subtotal) {
        this.id = id;
        this.saleId = saleId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public SaleTransactionItem(String productName, double unitPrice, int quantity, double subtotal) {
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSaleId() {
        return saleId;
    }

    public void setSaleId(long saleId) {
        this.saleId = saleId;
    }

    public String getProductName() {
        return productName;
    }

    public String getItemName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
