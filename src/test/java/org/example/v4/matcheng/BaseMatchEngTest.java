package org.example.v4.matcheng;

import org.example.v4.common.MatcherEventType;
import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.order.Order;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class BaseMatchEngTest {



    // ------------------------------- UTILITY METHODS --------------------------
    public void checkEventTrade(Order incoming, int index, long matchedId, long price, long size) {
        MatcherTradeEvent event = incoming.matcherTradeEvents.get(index);
        assertThat(event.eventType, is(MatcherEventType.TRADE));
        assertThat(event.matchedOrderId, is(matchedId));
        assertThat(event.matchedPrice, is(price));
        assertThat(event.size, is(size));
    }

    public void checkEventRejection(Order incoming, int index, long size) {
        MatcherTradeEvent event = incoming.matcherTradeEvents.get(index);
        assertThat(event.eventType, is(MatcherEventType.REJECT));
        assertThat(event.size, is(size));
        assertThat(event.matchedOrderId, is(0L));
        assertThat(event.matchedPrice, is(0L));
    }

    public void checkEventReduce(Order incoming, int index, long size) {
        MatcherTradeEvent event = incoming.matcherTradeEvents.get(index);
        assertThat(event.eventType, is(MatcherEventType.REDUCE));
        assertThat(event.size, is(size));
        assertThat(event.matchedOrderId, is(0L));
        assertThat(event.matchedPrice, is(0L));
    }
}
