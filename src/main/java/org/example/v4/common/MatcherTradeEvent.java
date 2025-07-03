package org.example.v4.common;


import org.example.v4.order.Order;

public final class MatcherTradeEvent {

    public MatcherEventType eventType;
    public long takerOrderId;
    public long makerOrderId;
    public long price;
    public long size;
    public boolean takerCompleted;
    public boolean makerCompleted;

    public static MatcherTradeEvent createTradeEvent(Order taker, Order maker, long price, long size) {
        MatcherTradeEvent event = new MatcherTradeEvent();
        event.eventType = MatcherEventType.TRADE;
        event.takerOrderId = taker.orderId;
        event.makerOrderId = maker.orderId;
        event.price = price;
        event.size = size;
        event.takerCompleted = taker.remainingQuantity == 0;
        event.makerCompleted = maker.remainingQuantity == 0;
        return event;
    }

    public static MatcherTradeEvent createRejectEvent(Order taker, long rejectedSize) {
        MatcherTradeEvent event = new MatcherTradeEvent();
        event.eventType = MatcherEventType.REJECT;
        event.takerOrderId = taker.orderId;
        event.price = taker.price;
        event.size = rejectedSize;
        event.takerCompleted = true;
        return event;
    }

    public static MatcherTradeEvent createReduceEvent(Order taker, long reduceSize, boolean takerCompleted) {
        MatcherTradeEvent event = new MatcherTradeEvent();
        event.eventType = MatcherEventType.REDUCE;
        event.takerOrderId = taker.orderId;
        event.price = taker.price;
        event.size = reduceSize;
        event.takerCompleted = takerCompleted;
        return event;
    }

}
