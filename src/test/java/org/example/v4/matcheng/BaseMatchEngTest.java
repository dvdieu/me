package org.example.v4.matcheng;

import org.example.v4.common.MatcherEventType;
import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.common.command.CommandResultCode;
import org.example.v4.common.command.OrderCommand;
import org.example.v4.order.Order;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseMatchEngTest {

    public void processAndValidate(MatchEng matchEng, OrderCommand cmd, CommandResultCode expectedCmdState) {
        CommandResultCode resultCode = MatchEng.processCommand(matchEng, cmd);
        assertThat(resultCode, is(expectedCmdState));
        matchEng.validateInternalState();
    }

    // ------------------------------- UTILITY METHODS --------------------------
    public void checkEventTrade(OrderCommand cmd, int index, long takerOrderId, long makerOrderId, long price, long size) {
        MatcherTradeEvent event = cmd.extractEvents().get(index);
        assertThat(event.eventType, is(MatcherEventType.TRADE));
        assertThat(event.takerOrderId, is(takerOrderId));
        assertThat(event.makerOrderId, is(makerOrderId));
        assertThat(event.price, is(price));
        assertThat(event.size, is(size));
    }

    public void checkEventRejection(OrderCommand cmd, int index, long size) {
        MatcherTradeEvent event = cmd.extractEvents().get(index);
        assertThat(event.eventType, is(MatcherEventType.REJECT));
        assertThat(event.size, is(size));
        assertThat(event.takerOrderId, is(cmd.orderId));
        assertThat(event.makerOrderId, is(0L));
        assertTrue(event.takerCompleted);
    }

    public void checkEventRejection(OrderCommand cmd, int index, long takerOrderId, long size) {
        MatcherTradeEvent event = cmd.extractEvents().get(index);
        assertThat(event.eventType, is(MatcherEventType.REJECT));
        assertThat(event.size, is(size));
        assertThat(event.takerOrderId, is(takerOrderId));
        assertThat(event.makerOrderId, is(0L));
        assertTrue(event.takerCompleted);
    }

    public void checkEventReduce(OrderCommand cmd, int index, long size) {
        MatcherTradeEvent event = cmd.extractEvents().get(index);
        assertThat(event.eventType, is(MatcherEventType.REDUCE));
        assertThat(event.size, is(size));
        assertThat(event.takerOrderId, is(cmd.orderId));
        assertThat(event.makerOrderId, is(0L));
    }
}
