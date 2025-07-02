package org.example.v4.orderbook;

import org.example.v4.order.Order;

import java.util.HashMap;
import java.util.Map;

public class PriceLevel {

    public long price;
    public DirectOrder head;
    public DirectOrder tail;

    public long numOrders;
    public long remainingQuantity;
    public long displayedQuantity;
    public Map<Long, Long> remainQuantityByUsers = new HashMap<>();
    public Map<Long, Long> displayedQuantityByUsers = new HashMap<>();

    public PriceLevel(long price) {
        this.price = price;
    }

    public DirectOrder addOrder(Order order) {
        numOrders++;
        remainingQuantity += order.remainingQuantity;
        displayedQuantity += order.displayedQuantity;
        remainQuantityByUsers.merge(order.userId, order.remainingQuantity, Long::sum);
        displayedQuantityByUsers.merge(order.userId, order.displayedQuantity, Long::sum);

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

    public long getRemainingQuantityWithoutUser(long userId) {
        return remainingQuantity - remainQuantityByUsers.getOrDefault(userId, 0L);
    }

    public long getDisplayedQuantityWithoutUser(long userId) {
        return displayedQuantity - displayedQuantityByUsers.getOrDefault(userId, 0L);
    }

    public void removeOrderVolume(Order order) {
        numOrders--;

        if(order.displayedQuantity > 0) {
            displayedQuantity -= order.displayedQuantity;

            displayedQuantityByUsers.computeIfPresent(order.userId, (k, v) -> {
                long l = v - order.displayedQuantity;
                return l > 0 ? l : null;
            });
        }

        if(order.remainingQuantity > 0) {
            remainingQuantity -= order.remainingQuantity;

            remainQuantityByUsers.computeIfPresent(order.userId, (k, v) -> {
                long l = v - order.remainingQuantity;
                return l > 0 ? l : null;
            });
        }
    }

    public void removeTradeVolume(long userId, long tradeSize) {
        remainingQuantity -= tradeSize;
        displayedQuantity -= tradeSize;

        remainQuantityByUsers.computeIfPresent(userId, (k, v) -> {
            long l = v - tradeSize;
            return l > 0 ? l : null;
        });

        displayedQuantityByUsers.computeIfPresent(userId, (k, v) -> {
            long l = v - tradeSize;
            return l > 0 ? l : null;
        });
    }
}
