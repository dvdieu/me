package org.example.v4.orderbook;

import org.example.v4.order.Order;

public class DirectOrder {

    public Order order;
    public PriceLevel priceLevel;

    public DirectOrder prev;
    public DirectOrder next;

    public DirectOrder(Order order, PriceLevel priceLevel) {
        this.order = order;
        this.priceLevel = priceLevel;
    }

    public void remove() {
        if(prev != null) {
            prev.next = next;
        }

        if(next != null) {
            next.prev = prev;
        }

        if(this == priceLevel.head) {
            priceLevel.head = prev;
        }

        if(this == priceLevel.tail) {
            priceLevel.tail = next;
        }
    }
}
