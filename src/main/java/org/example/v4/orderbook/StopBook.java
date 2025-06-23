package org.example.v4.orderbook;

import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;
import org.example.v4.order.OrderType;

import java.util.*;

public class StopBook {

    private final TreeMap<Long, PriceLevel> sellStopLevels = new TreeMap<>();
    private final TreeMap<Long, PriceLevel> buyStopLevels = new TreeMap<>();


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

    public long calculateLiquidity(Order incoming, long prevPrice, long newPrice) {
        if (prevPrice == newPrice) {
            return 0;
        }

        long totalLiquidity = 0;
        if (newPrice > prevPrice) {
            NavigableMap<Long, PriceLevel> subMap = sellStopLevels.subMap(prevPrice, false, newPrice, true);
            for (PriceLevel priceLevel : subMap.values()) {
                for (Order order : priceLevel.orders) {
                    if (!incoming.isSelfMatch(order) && !(order.type == OrderType.LIMIT && order.price > newPrice)) {
                        totalLiquidity += order.remainingQuantity;
                    }
                }
            }
        } else {
            NavigableMap<Long, PriceLevel> subMap = buyStopLevels.subMap(newPrice, true, prevPrice, false);
            for (PriceLevel priceLevel : subMap.values()) {
                for (Order order : priceLevel.orders) {
                    if (!incoming.isSelfMatch(order) && !(order.type == OrderType.LIMIT && order.price < newPrice)) {
                        totalLiquidity += order.remainingQuantity;
                    }
                }
            }
        }

        return totalLiquidity;
    }

    public List<Order> getTriggeredStopOrders(long prevPrice, long newPrice) {
        if (newPrice == prevPrice) {
            return Collections.emptyList();
        }

        List<Order> triggered = new ArrayList<>();
        if(newPrice > prevPrice) {
            collectTriggeredOrders(buyStopLevels.subMap(prevPrice, false, newPrice, true), triggered);
            collectTriggeredOrders(sellStopLevels.subMap(prevPrice, false, newPrice, true), triggered);
        } else {
            collectTriggeredOrders(buyStopLevels.subMap(newPrice, true, prevPrice, false), triggered);
            collectTriggeredOrders(sellStopLevels.subMap(newPrice, true, prevPrice, false), triggered);
        }

        triggered.sort(Comparator.comparingLong(order -> order.id));
        return triggered;
    }

    private void collectTriggeredOrders(SortedMap<Long, PriceLevel> levels, List<Order> out) {
        if (levels.isEmpty()) {
            return;
        }

        List<Long> stopPrices = new ArrayList<>();
        for (Map.Entry<Long, PriceLevel> entry : levels.entrySet()) {
            stopPrices.add(entry.getKey());
            out.addAll(entry.getValue().orders);
        }

        Order order = levels.firstEntry().getValue().orders.getFirst();
        if (order.side == OrderSide.BUY) {
            for (Long stopPrice : stopPrices) {
                buyStopLevels.remove(stopPrice);
            }
        } else {
            for (Long stopPrice : stopPrices) {
                sellStopLevels.remove(stopPrice);
            }
        }
    }


    public void logStopBookState() {
        if (sellStopLevels.isEmpty() && buyStopLevels.isEmpty()) {
            return;
        }

        System.out.println("\nStop Book:");
        if (!sellStopLevels.isEmpty()) {
            System.out.print("Sell triggers: \t");
            for (Map.Entry<Long, PriceLevel> entry : sellStopLevels.entrySet()) {
                System.out.print(entry.getKey() + "[" + entry.getValue().orders.size() + "] \t");
            }
            System.out.println();
        }

        if (!buyStopLevels.isEmpty()) {
            System.out.println("Buy triggers : \t");
            for (Map.Entry<Long, PriceLevel> entry : buyStopLevels.entrySet()) {
                System.out.print(entry.getKey() + "[" + entry.getValue().orders.size() + "] \t");
            }
            System.out.println();
        }
    }
}
