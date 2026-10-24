package com.example.demo.dto;

import java.math.BigDecimal;

public final class DashboardStats {

    private final long totalBooks;
    private final long customers;
    private final long orders;
    private final long pendingOrders;
    private final long unitsInStock;
    private final long lowStockBooks;
    private final BigDecimal deliveredRevenue;

    public DashboardStats(long totalBooks, long customers, long orders, long pendingOrders, long unitsInStock, long lowStockBooks, BigDecimal deliveredRevenue) {
        this.totalBooks = totalBooks;
        this.customers = customers;
        this.orders = orders;
        this.pendingOrders = pendingOrders;
        this.unitsInStock = unitsInStock;
        this.lowStockBooks = lowStockBooks;
        this.deliveredRevenue = deliveredRevenue;
    }

    public long getTotalBooks() { return totalBooks; }
    public long getCustomers() { return customers; }
    public long getOrders() { return orders; }
    public long getPendingOrders() { return pendingOrders; }
    public long getUnitsInStock() { return unitsInStock; }
    public long getLowStockBooks() { return lowStockBooks; }
    public BigDecimal getDeliveredRevenue() { return deliveredRevenue; }

}
