package org.example.matching.context;

import org.example.common.command.OrderCommand;
import org.example.order.Order;
import org.example.orderbook.DirectOrder;
import org.example.orderbook.PriceLevel;

import java.util.ArrayList;
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

        if(resting.displayedQuantity == 0) {
            Order icebergChild = resting.createIcebergChild();
            if(icebergChild != null) {
                refilledOrders.add(icebergChild);
            }
        }
    }

}
