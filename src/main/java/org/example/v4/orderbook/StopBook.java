package org.example.v4.orderbook;

import org.example.v4.order.Order;
import org.example.v4.order.OrderType;
import org.example.v4.order.TimeInForce;

import java.util.*;

public class StopBook {

    private final TreeMap<Long, PriceLevel> stopLevels = new TreeMap<>();

    private PriceLevel getOrCreateLevel(long price) {
        return stopLevels.computeIfAbsent(price, PriceLevel::new);
    }

    public void addStopOrder(Order order) {
        PriceLevel level = getOrCreateLevel(order.stopPrice);
        level.addOrder(order);
    }

    public void removeOrder(Order order) {
        PriceLevel priceLevel = stopLevels.get(order.stopPrice);
        if(priceLevel != null) {
            priceLevel.orders.remove(order);
            if(priceLevel.orders.isEmpty()) {
                stopLevels.remove(order.price);
            }
        }
    }

    public long calculateLiquidity(Order incoming, long prevPrice, long newPrice) {
        if (prevPrice == newPrice) {
            return 0;
        }

        NavigableMap<Long, PriceLevel> subMap;
        if(newPrice > prevPrice) {
            subMap = stopLevels.subMap(prevPrice, false, newPrice, true);
        } else {
            subMap = stopLevels.subMap(newPrice, true, prevPrice, false);
        }

        long totalLiquidity = 0;
        for (PriceLevel priceLevel : subMap.values()) {
            for (Order order : priceLevel.orders) {
                if (!incoming.isSelfMatch(order) && order.timeInForce != TimeInForce.FOK
                        && (order.type == OrderType.STOP_MARKET || incoming.isPriceAcceptable(order.price))) {
                    totalLiquidity += order.remainingQuantity;
                }
            }
        }

        return totalLiquidity;
    }

    public List<Order> getTriggeredStopOrders(long prevPrice, long newPrice) {
        if (newPrice == prevPrice) {
            return Collections.emptyList();
        }

        NavigableMap<Long, PriceLevel> subMap;
        if(newPrice > prevPrice) {
            subMap = stopLevels.subMap(prevPrice, false, newPrice, true);
        } else {
            subMap = stopLevels.subMap(newPrice, true, prevPrice, false).reversed();
        }

        List<Order> triggered = new ArrayList<>();
        Iterator<Map.Entry<Long, PriceLevel>> iterator = subMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PriceLevel> entry = iterator.next();
            triggered.addAll(entry.getValue().orders);

            iterator.remove();
        }

        return triggered;
    }

    public void logStopBookState() {
        if (stopLevels.isEmpty()) {
            return;
        }

        System.out.println("\nStop Book:");
        System.out.print("Price triggers: \t");
        for (Map.Entry<Long, PriceLevel> entry : stopLevels.entrySet()) {
            System.out.print(entry.getKey() + "[" + entry.getValue().orders.size() + "] \t");
        }
        System.out.println();
    }
}
