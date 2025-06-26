package org.example.v4.matcheng;

import org.example.v4.common.MatcherEventType;
import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.order.Order;
import org.example.v4.common.L2MarketData;
import org.junit.jupiter.api.Test;

import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.LIMIT;
import static org.example.v4.order.OrderType.MARKET;
import static org.example.v4.order.TimeInForce.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


class MatchEngTest {

    @Test
    void shouldMatchFully() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 110, 10));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 5);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{110},
                new long[]{10},
                new long[]{1},
                new long[]{100},
                new long[]{5},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventTrade(incoming, 0, 1, 100, 5);
    }

    @Test
    void shouldMatchGTCPartially() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(2, 2, SELL, LIMIT, GTC, 100, 10);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{100},
                new long[]{5},
                new long[]{1},
                new long[]{},
                new long[]{},
                new long[]{}
        );

        assertEquals(expected, snapshot);
        assertEquals(5, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventTrade(incoming, 0, 1, 100, 5);
    }

    @Test
    void shouldMatchIOCPartially() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(2, 2, SELL, MARKET, IOC, 100, 10);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{}
        );

        assertEquals(expected, snapshot);
        assertEquals(5, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(2));
        checkEventRejection(incoming, 0, 5);
        checkEventTrade(incoming, 1, 1, 100, 5);
    }

    @Test
    void shouldNoMatchGTC() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 95, 10));

        Order incoming = Order.createStandardOrder(2, 2, SELL, LIMIT, GTC, 100, 10);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{100},
                new long[]{10},
                new long[]{1},
                new long[]{95},
                new long[]{10},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(10, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(0));
    }

    @Test
    void shouldNoMatchIOC() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 95, 10));

        Order incoming = Order.createStandardOrder(2, 2, SELL, MARKET, IOC, 100, 10);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{95},
                new long[]{10},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(10, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventRejection(incoming, 0, 10);
    }

    @Test
    void shouldNoMatchFOK() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 110, 10));

        Order incoming = Order.createStandardOrder(3, 2, SELL, MARKET, FOK, 0, 25);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{110, 100},
                new long[]{10, 10},
                new long[]{1, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(25, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventRejection(incoming, 0, 25);
    }

    @Test
    void shouldMatchMultiplePriceLevels() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 101, 5));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 9);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{100},
                new long[]{1},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(2));
        checkEventTrade(incoming, 0, 1, 101, 5);
        checkEventTrade(incoming, 1, 2, 100, 4);
    }

    @Test
    void shouldRefilledIcebergs() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createIcebergOrder(1, 1, BUY, 100, 10, 2));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 10);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{100},
                new long[]{5},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(4));
        checkEventTrade(incoming, 0, 1, 100, 2);
        checkEventTrade(incoming, 1, 2, 100, 5);
        checkEventTrade(incoming, 2, 1, 100, 2);
        checkEventTrade(incoming, 3, 1, 100, 1);
    }


    @Test
    public void shouldAddPostOnlyOrders() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 10));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 110, 10).postOnly();
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{110},
                new long[]{10},
                new long[]{1},
                new long[]{100},
                new long[]{15},
                new long[]{2}
        );

        assertEquals(expected, snapshot);
        assertEquals(10, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(0));
    }


    @Test
    public void shouldRejectPostOnlyOrders() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 10));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 90, 10).postOnly();
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{100},
                new long[]{15},
                new long[]{2}
        );

        assertEquals(expected, snapshot);
        assertEquals(10, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventRejection(incoming, 0, 10);
    }

    //=============================================================================================
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
}
