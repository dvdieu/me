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
        if(head == null) {
            head = new DirectOrder(order, this);
            tail = head;

            return head;
        } else {
            DirectOrder newTail = new DirectOrder(order, this);
            tail.prev = newTail;
            newTail.next = tail;
            tail = newTail;

            return newTail;
        }
    }

    public boolean isEmpty() {
        return head == null;
    }

    public Stream<DirectOrder> orderStream() {
        return StreamSupport.stream(new OrdersSpliterator(head), false);
    }
}
