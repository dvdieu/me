package org.example.v4.orderbook;

import org.example.v4.order.Order;

import java.util.Deque;
import java.util.LinkedList;

public class PriceLevel {

    public long price;
    public Deque<Order> orders = new LinkedList<>();

    public PriceLevel(long price) {
        this.price = price;
    }

    public void addOrder(Order order) {
        orders.add(order);
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }
}
