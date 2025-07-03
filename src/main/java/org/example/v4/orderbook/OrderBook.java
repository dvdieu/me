package org.example.v4.orderbook;

import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;
import org.example.v4.common.L2MarketData;

import java.util.Collections;
import java.util.TreeMap;

public class OrderBook {

    private final TreeMap<Long, PriceLevel> sellLevels = new TreeMap<>();
    private final TreeMap<Long, PriceLevel> buyLevels = new TreeMap<>(Collections.reverseOrder());


    public TreeMap<Long, PriceLevel> getLevels(OrderSide side) {
        return side == OrderSide.BUY ? buyLevels : sellLevels;
    }

    private PriceLevel getOrCreateLevel(OrderSide side, long price) {
        return getLevels(side).computeIfAbsent(price, PriceLevel::new);
    }

    public PriceLevel getBestLevel(OrderSide side) {
        TreeMap<Long, PriceLevel> levels = getLevels(side);
        if(levels.isEmpty()) {
            return null;
        }

        return levels.firstEntry().getValue();
    }


    public DirectOrder addOrder(Order order) {
        PriceLevel level = getOrCreateLevel(order.side, order.price);
        return level.addOrder(order);
    }

    public void removeLevel(OrderSide side, long price) {
        TreeMap<Long, PriceLevel> levels = getLevels(side);
        levels.remove(price);
    }

    public void removeOrder(DirectOrder order) {
        order.remove();

        if(order.priceLevel.isEmpty()) {
            getLevels(order.side).remove(order.price);
        }
    }


    public void logOrderBookState() {
        System.out.println("Order Book:");

        System.out.print("SELL: \t");
        if(sellLevels.isEmpty()) {
            System.out.print("(empty)");
        } else {
            sellLevels.values().forEach(this::logPriceLevel);
        }
        System.out.println();

        System.out.print("BUY : \t");
        if(buyLevels.isEmpty()) {
            System.out.print("(empty)");
        } else {
            buyLevels.values().forEach(this::logPriceLevel);
        }

        System.out.println();
    }


    private void logPriceLevel(PriceLevel level) {
        long quantity = level.remainingQuantity;
        long count = level.numOrders;
        System.out.printf("%d(%d,%d) \t", level.price, quantity, count);
    }

    public L2MarketData getL2MarketDataSnapshot() {
        final int asksSize = sellLevels.size();
        final int bidsSize = buyLevels.size();
        final L2MarketData data = new L2MarketData(asksSize, bidsSize);
        fillAsks(data);
        fillBids(data);
        return data;
    }

    private void fillAsks(L2MarketData data) {
        data.askSize = 0;
        sellLevels.forEach((p, bucket) -> {
            final int i = data.askSize++;
            data.askPrices[i] = bucket.price;
            data.askVolumes[i] = bucket.remainingQuantity;
            data.askOrders[i] = bucket.numOrders;
        });
    }

    private void fillBids(L2MarketData data) {
        data.bidSize = 0;
        buyLevels.forEach((p, bucket) -> {
            final int i = data.bidSize++;
            data.bidPrices[i] = bucket.price;
            data.bidVolumes[i] = bucket.remainingQuantity;
            data.bidOrders[i] = bucket.numOrders;
        });
    }
}
