package org.exchange.core.util;

public class Order {
    public long orderId;
    public long qty;
    public long price;
    public boolean isBuy; // New field
    public Order next = null;
    public Order prev= null;

    public Order(long orderId, long qty, long price, boolean isBuy) {
        this.orderId = orderId;
        this.isBuy = isBuy; // Explicitly set even in constructor
        this.qty = qty;
        this.price = price;
    }

    public void update(long orderId, long qty, long price, boolean isBuy) {
        this.orderId = orderId;
        this.qty = qty;
        this.isBuy = isBuy; // Explicitly set the side
        this.price = price;
        this.next = null;
        this.prev = null;
    }

}
