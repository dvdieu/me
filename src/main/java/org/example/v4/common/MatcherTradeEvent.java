package org.example.v4.common;


public final class MatcherTradeEvent {

    public MatcherEventType eventType;
    public long matchedOrderId;
    public long matchedPrice;
    public long size;


    public static MatcherTradeEvent createTradeEvent(long matchedOrderId, long matchedPrice, long size) {
        MatcherTradeEvent event = new MatcherTradeEvent();
        event.eventType = MatcherEventType.TRADE;
        event.matchedOrderId = matchedOrderId;
        event.matchedPrice = matchedPrice;
        event.size = size;
        return event;
    }

    public static MatcherTradeEvent createRejectEvent(long size) {
        MatcherTradeEvent event = new MatcherTradeEvent();
        event.eventType = MatcherEventType.REJECT;
        event.size = size;
        return event;
    }

    public static MatcherTradeEvent createReduceEvent(long size) {
        MatcherTradeEvent event = new MatcherTradeEvent();
        event.eventType = MatcherEventType.REDUCE;
        event.size = size;
        return event;
    }

}
