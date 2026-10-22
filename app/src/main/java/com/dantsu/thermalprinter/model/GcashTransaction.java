package com.dantsu.thermalprinter.model;

public class GcashTransaction {
    public static final String TYPE_CASH_IN = "CASH_IN";
    public static final String TYPE_CASH_OUT = "CASH_OUT";

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_VOIDED = "VOIDED";

    private long id;
    private String type;
    private String refNo;
    private String customerNumber;
    private String customerName;
    private double amount;
    private double fee;
    private double totalCashHandled;
    private String dateTime;
    private String dateOnly;
    private String note;
    private String status;

    public GcashTransaction() {
        this.status = STATUS_COMPLETED;
    }

    public GcashTransaction(String type, String refNo, String customerNumber, String customerName,
                            double amount, double fee, double totalCashHandled,
                            String dateTime, String dateOnly, String note, String status) {
        this.type = type;
        this.refNo = refNo;
        this.customerNumber = customerNumber;
        this.customerName = customerName;
        this.amount = amount;
        this.fee = fee;
        this.totalCashHandled = totalCashHandled;
        this.dateTime = dateTime;
        this.dateOnly = dateOnly;
        this.note = note;
        this.status = (status != null && !status.isEmpty()) ? status : STATUS_COMPLETED;
    }

    public GcashTransaction(long id, String type, String refNo, String customerNumber, String customerName,
                            double amount, double fee, double totalCashHandled,
                            String dateTime, String dateOnly, String note, String status) {
        this(type, refNo, customerNumber, customerName, amount, fee, totalCashHandled, dateTime, dateOnly, note, status);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isCashIn() {
        return TYPE_CASH_IN.equalsIgnoreCase(type);
    }

    public boolean isCashOut() {
        return TYPE_CASH_OUT.equalsIgnoreCase(type);
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public double getTotalCashHandled() {
        return totalCashHandled;
    }

    public void setTotalCashHandled(double totalCashHandled) {
        this.totalCashHandled = totalCashHandled;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isVoided() {
        return STATUS_VOIDED.equalsIgnoreCase(status);
    }
}
