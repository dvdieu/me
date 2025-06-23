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
        PriceLevel level = getOrCreateLevel(order.side, order.stopPrice);
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
        for (Map.Entry<Long, PriceLevel> longPriceLevelEntry : levels.entrySet()) {
            PriceLevel priceLevel = longPriceLevelEntry.getValue();

            out.addAll(priceLevel.orders);

            Order order = priceLevel.orders.getFirst();
            if(order.side == OrderSide.BUY) {
                buyStopLevels.remove(order.stopPrice);
            } else {
                sellStopLevels.remove(order.stopPrice);
            }
        }
    }


    public void logStopBookState() {
        if(sellStopLevels.isEmpty() && buyStopLevels.isEmpty()) {
            return;
        }

        System.out.println("\nStop Book:");
        if(!sellStopLevels.isEmpty()) {
            System.out.print("Sell triggers: \t");
            for (Map.Entry<Long, PriceLevel> entry : sellStopLevels.entrySet()) {
                System.out.print(entry.getKey() + "[" + entry.getValue().orders.size() + "] \t");
            }
            System.out.println();
        }

        if(!buyStopLevels.isEmpty()) {
            System.out.println("Buy triggers : \t");
            for (Map.Entry<Long, PriceLevel> entry : buyStopLevels.entrySet()) {
                System.out.print(entry.getKey() + "[" + entry.getValue().orders.size() + "] \t");
            }
            System.out.println();
        }
    }
}
