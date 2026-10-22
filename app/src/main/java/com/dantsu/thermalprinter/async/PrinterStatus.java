package com.dantsu.thermalprinter.async;

public class PrinterStatus {
    private AsyncEscPosPrinter asyncEscPosPrinter;
    private int printerStatus;

    public PrinterStatus(AsyncEscPosPrinter asyncEscPosPrinter, int printerStatus) {
        this.asyncEscPosPrinter = asyncEscPosPrinter;
        this.printerStatus = printerStatus;
    }

    public AsyncEscPosPrinter getAsyncEscPosPrinter() {
        return asyncEscPosPrinter;
    }

    public int getPrinterStatus() {
        return printerStatus;
    }
}
