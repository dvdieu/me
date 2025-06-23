package org.example.v4.matching.context;

import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.order.Order;
import org.example.v4.orderbook.PriceLevel;

import java.util.*;
import java.util.function.Consumer;

public class MatchingContext {

    public Order incoming;
    public PriceLevel priceLevel;
    public long bucketRemaining;
    public long lastTradePrice;

    public List<Order> selfMatchOrders = new ArrayList<>();
    public List<Order> refilledOrders = new ArrayList<>();



    public void initContext(Order incoming) {
        this.incoming = incoming;
        this.selfMatchOrders.clear();
    }

    public void updatePriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel;
        this.refilledOrders.clear();
        this.lastTradePrice = 0;
        this.bucketRemaining = priceLevel.orders.stream()
                .filter(o -> !incoming.isSelfMatch(o))
                .mapToLong(o -> o.displayedQuantity).sum();
    }


    public void performMatch(Iterator<Order> iterator, Order resting, long tradeSize) {
        this.bucketRemaining -= tradeSize;
        this.lastTradePrice = resting.price;

        incoming.matching(resting, tradeSize);
        System.out.printf("Trade: %s (Maker) %d vs %s (Taker) %d @%d => %d\n",
                resting.side, resting.id, incoming.side, incoming.id, priceLevel.price, tradeSize);

        incoming.matcherTradeEvents.add(MatcherTradeEvent.createTradeEvent(resting.id, resting.price, tradeSize));

        if(resting.displayedQuantity == 0) {
            iterator.remove();

            Order icebergChild = resting.createIcebergChild();
            if(icebergChild != null) {
                refilledOrders.add(icebergChild);
            }
        }
    }

}
