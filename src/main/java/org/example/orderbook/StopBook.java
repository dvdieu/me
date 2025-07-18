package org.example.orderbook;

import org.example.order.Order;
import org.example.order.OrderType;
import org.example.order.TimeInForce;

import java.util.*;

/**
 * StopBook manages all pending stop orders in the matching engine.
 *
 * <p>Stop orders are stored in a price-sorted structure and are not active
 * until their trigger price is crossed. This class provides logic to:</p>
 * <ul>
 *   <li>Add/remove stop orders</li>
 *   <li>Trigger stop orders based on price movement</li>
 *   <li>Calculate stop liquidity for FOK checks</li>
 *   <li>Log internal stop order state</li>
 * </ul>
 */
public class StopBook {

    private final TreeMap<Long, PriceLevel> stopLevels = new TreeMap<>();

    private PriceLevel getOrCreateLevel(long price) {
        return stopLevels.computeIfAbsent(price, PriceLevel::new);
    }

    public TreeMap<Long, PriceLevel> getStopLevels() {
        return stopLevels;
    }

    public DirectOrder addStopOrder(Order order) {
        PriceLevel level = getOrCreateLevel(order.stopPrice);
        return level.addOrder(order);
    }

    public void removeOrder(DirectOrder order) {
        order.remove();
        if(order.priceLevel.isEmpty()) {
            stopLevels.remove(order.stopPrice);
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
            DirectOrder order = priceLevel.head;
            while (order != null) {
                if (incoming.side != order.side && order.timeInForce != TimeInForce.FOK && !incoming.isSelfMatch(order)
                        && (order.type == OrderType.STOP_MARKET || incoming.isPriceAcceptable(order.price))) {
                    totalLiquidity += order.remainingQuantity;
                }

                order = order.prev;
            }
        }

        return totalLiquidity;
    }

    public List<DirectOrder> getTriggeredStopOrders(long prevPrice, long newPrice) {
        if (newPrice == prevPrice) {
            return Collections.emptyList();
        }

        NavigableMap<Long, PriceLevel> subMap;
        if(newPrice > prevPrice) {
            subMap = stopLevels.subMap(prevPrice, false, newPrice, true);
        } else {
            subMap = stopLevels.subMap(newPrice, true, prevPrice, false).reversed();
        }

        List<DirectOrder> triggered = new ArrayList<>();
        Iterator<Map.Entry<Long, PriceLevel>> iterator = subMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PriceLevel> entry = iterator.next();
            DirectOrder directOrder = entry.getValue().head;
            while (directOrder != null) {
                triggered.add(directOrder);
                directOrder = directOrder.prev;
            }

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
            System.out.print(entry.getKey() + "[" + entry.getValue().numOrders + "] \t");
        }
        System.out.println();
    }
}
