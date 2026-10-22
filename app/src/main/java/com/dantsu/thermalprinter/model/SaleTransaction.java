package com.dantsu.thermalprinter.model;

import java.util.ArrayList;
import java.util.List;

public class SaleTransaction {
    private long id;
    private String receiptNo;
    private String dateTime;
    private String dateOnly;
    private double totalAmount;
    private String paymentMode; // CASH, GCASH, CREDIT
    private double amountPaid;
    private double changeAmount;
    private String customerNote;
    private boolean isSettled; // true if paid/settled, false if outstanding credit
    private List<SaleTransactionItem> items = new ArrayList<>();

    public SaleTransaction() {
        this.isSettled = true;
    }

    public SaleTransaction(String receiptNo, String dateTime, String dateOnly, double totalAmount, String paymentMode, double amountPaid, double changeAmount, String customerNote, boolean isSettled) {
        this.receiptNo = receiptNo;
        this.dateTime = dateTime;
        this.dateOnly = dateOnly;
        this.totalAmount = totalAmount;
        this.paymentMode = paymentMode;
        this.amountPaid = amountPaid;
        this.changeAmount = changeAmount;
        this.customerNote = customerNote;
        this.isSettled = isSettled;
    }

    public SaleTransaction(long id, String receiptNo, String dateTime, String dateOnly, double totalAmount, String paymentMode, double amountPaid, double changeAmount, String customerNote, boolean isSettled) {
        this.id = id;
        this.receiptNo = receiptNo;
        this.dateTime = dateTime;
        this.dateOnly = dateOnly;
        this.totalAmount = totalAmount;
        this.paymentMode = paymentMode;
        this.amountPaid = amountPaid;
        this.changeAmount = changeAmount;
        this.customerNote = customerNote;
        this.isSettled = isSettled;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDateOnly() {
        return dateOnly;
    }

    public void setDateOnly(String dateOnly) {
        this.dateOnly = dateOnly;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public double getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(double changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getCustomerNote() {
        return (customerNote == null) ? "" : customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public boolean isSettled() {
        return isSettled;
    }

    public void setSettled(boolean settled) {
        isSettled = settled;
    }

    public List<SaleTransactionItem> getItems() {
        return items;
    }

    public void setItems(List<SaleTransactionItem> items) {
        this.items = items;
    }
}
