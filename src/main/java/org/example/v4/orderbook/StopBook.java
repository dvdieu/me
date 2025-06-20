package org.example.v4.orderbook;

import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;

import java.util.*;

public class StopBook {

    private final TreeMap<Long, PriceLevel> sellStopLevels = new TreeMap<>();
    private final TreeMap<Long, PriceLevel> buyStopLevels = new TreeMap<>(Collections.reverseOrder());


    private TreeMap<Long, PriceLevel> getLevels(OrderSide side) {
        return side == OrderSide.BUY ? buyStopLevels : sellStopLevels;
    }

    private PriceLevel getOrCreateLevel(OrderSide side, long price) {
        return getLevels(side).computeIfAbsent(price, PriceLevel::new);
    }


    public void addStopOrder(Order order) {
        PriceLevel level = getOrCreateLevel(order.side, order.price);
        level.addOrder(order);
    }

    public List<Order> getTriggeredStopOrders(long prevPrice, long newPrice) {
        List<Order> triggered = new ArrayList<>();
        if(newPrice == prevPrice) {
            return triggered;
        }

        if (newPrice > prevPrice) {
            collectTriggeredOrders(buyStopLevels.subMap(newPrice, true, prevPrice, false), triggered);
            collectTriggeredOrders(sellStopLevels.subMap(prevPrice, false, newPrice, true), triggered);
        } else {
            collectTriggeredOrders(buyStopLevels.subMap(prevPrice, false, newPrice, true), triggered);
            collectTriggeredOrders(sellStopLevels.subMap(newPrice, true, prevPrice, false), triggered);
        }

        triggered.sort(Comparator.comparingLong(order -> order.id));
        return triggered;
    }

    private void collectTriggeredOrders(SortedMap<Long, PriceLevel> levels, List<Order> out) {
        for (PriceLevel level : levels.values()) {
            out.addAll(level.orders);
        }
    }


    public void logStopBookState() {
        if(sellStopLevels.isEmpty() && buyStopLevels.isEmpty()) {
            return;
        }

        System.out.println("Stop Book:");
        if(!sellStopLevels.isEmpty()) {
            System.out.println("Sell triggers: ");
            for (Map.Entry<Long, PriceLevel> entry : sellStopLevels.entrySet()) {
                System.out.println(entry.getKey() + "[" + entry.getValue().orders.size() + "] ");
            }
            System.out.println();
        }

        if(!buyStopLevels.isEmpty()) {
            System.out.println("Buy triggers: ");
            for (Map.Entry<Long, PriceLevel> entry : buyStopLevels.entrySet()) {
                System.out.println(entry.getKey() + "[" + entry.getValue().orders.size() + "] ");
            }
            System.out.println();
        }
    }
}
