package org.example.v4.matching.context;

import org.example.v4.common.command.OrderCommand;
import org.example.v4.order.Order;
import org.example.v4.orderbook.DirectOrder;
import org.example.v4.orderbook.PriceLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MatchingContext {

    public final MatchingCallback matchingCallback;

    public OrderCommand cmd;
    public Order incoming;
    public PriceLevel priceLevel;
    public long bucketRemaining;

    public List<Order> selfMatchOrders = new ArrayList<>();
    public List<Order> refilledOrders = new ArrayList<>();

    public MatchingContext(MatchingCallback matchingCallback) {
        this.matchingCallback = matchingCallback;
    }


    public void initContext(OrderCommand cmd, Order incoming) {
        this.cmd = cmd;
        this.incoming = incoming;
        this.selfMatchOrders.clear();
    }

    public void updatePriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel;
        this.refilledOrders.clear();
        this.bucketRemaining = priceLevel.getDisplayedQuantityWithoutUser(incoming.userId);
    }


    public void performMatch(DirectOrder resting, long tradeSize) {
        this.bucketRemaining -= tradeSize;
        matchingCallback.performMatch(cmd, incoming, resting, tradeSize);

        if(resting.order.displayedQuantity == 0) {
            Order icebergChild = resting.order.createIcebergChild();
            if(icebergChild != null) {
                refilledOrders.add(icebergChild);
            }
        }
    }

}
