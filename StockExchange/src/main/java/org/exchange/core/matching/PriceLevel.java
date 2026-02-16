package org.exchange.core.matching;

import org.exchange.core.matching.util.OrderLinkedList;
import org.exchange.core.util.Order;

public class PriceLevel {
    public OrderLinkedList orders;
    public long price;
    public long totalQty;

    public PriceLevel(long price) {
        this.price = price;
        this.orders = new OrderLinkedList();
        this.totalQty = 0;
    }

    public void addOrder(Order order) {
        this.orders.add(order);
        totalQty += order.qty;
    }

    public void removeOrder(Order order) {
        this.orders.remove(order);
        totalQty -= order.qty;
    }

    public long getPrice() {
        return price;
    }

    public OrderLinkedList getOrders() {
        return orders;
    }

    public boolean isEmpty() {
        return orders.head == null;
    }
}
