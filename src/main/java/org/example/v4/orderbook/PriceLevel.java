package org.example.v4.orderbook;

import org.example.v4.order.Order;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PriceLevel {

    public long price;
    public DirectOrder head;
    public DirectOrder tail;

    public PriceLevel(long price) {
        this.price = price;
    }

    public DirectOrder addOrder(Order order) {
        DirectOrder directOrder = new DirectOrder(order, this);

        if(head == null) {
            head = directOrder;
            tail = head;
        } else {
            tail.prev = directOrder;
            directOrder.next = tail;
            tail = directOrder;
        }

        return directOrder;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public Stream<DirectOrder> orderStream() {
        return StreamSupport.stream(new OrdersSpliterator(head), false);
    }
}
