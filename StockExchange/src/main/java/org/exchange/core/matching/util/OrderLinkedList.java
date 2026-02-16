package org.exchange.core.matching.util;

import org.exchange.core.util.Order;

public class OrderLinkedList {
    public Order head = null;
    public Order tail = null;

    public OrderLinkedList() {
    }

    public void add(Order order) {
        if (head == null) {
            head = tail = order;
        } else {
            tail.next = order;
            order.prev = tail;
            tail = order;
        }
    }

    public void remove(Order order) {
        if (order == null) return;

        if (order.prev != null) {
            order.prev.next = order.next;
        } else {
            // If head == order
            head = order.next;
        }

        if (order.next != null) {
            order.next.prev = order.prev;
        } else {
            tail = order.prev;
        }

        order.next = null;
        order.prev = null;
    }

    public void removeHead() {
        if (head == null) return;

        Order oldHead = head;
        head = head.next;

        if (head != null) {
            head.prev = null;
        } else {
            tail = null; // List is now empty
        }

        oldHead.next = null; // Clear pointer for reuse
    }
}
