package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.order.Order;
import org.junit.jupiter.api.Test;

import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.*;
import static org.example.v4.order.TimeInForce.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


class MatchEngStopOrderTest extends BaseMatchEngTest {

    @Test
    void shouldMatchBuyGTCAndTriggerStopOrder() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 95, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(4, 1, SELL, STOP_LIMIT, GTC, 95, 95, 5));
        matchEng.placeOrder(Order.createStopOrder(5, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10));

        Order triggerAndAddToCommandQueueOrder = Order.createStopOrder(8, 2, BUY, STOP_LIMIT, GTC, 95, 100, 1);
        matchEng.placeOrder(triggerAndAddToCommandQueueOrder);

        Order incoming = Order.createStandardOrder(100, 2, BUY, LIMIT, GTC, 96, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{97},
                new long[]{4},
                new long[]{1},
                new long[]{96, 90},
                new long[]{3, 10},
                new long[]{1, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(3, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(5));
        checkEventTrade(incoming, 0, 1, 94, 1);
        checkEventTrade(incoming, 1, 2, 96, 1);
        checkEventTrade(incoming, 2, 3, 96, 5);
        checkEventTrade(incoming, 3, 4, 96, 5);
        checkEventTrade(incoming, 4, 5, 96, 5);

        assertThat(triggerAndAddToCommandQueueOrder.matcherTradeEvents.size(), is(1));
        checkEventTrade(triggerAndAddToCommandQueueOrder, 0, 6, 97, 1);
    }

    @Test
    void shouldMatchBuyIOCAndTriggerStopOrder() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 95, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(4, 1, SELL, STOP_LIMIT, GTC, 95, 95, 5));
        matchEng.placeOrder(Order.createStopOrder(5, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10));

        Order triggerAndAddToCommandQueueOrder = Order.createStopOrder(8, 2, BUY, STOP_LIMIT, GTC, 95, 100, 1);
        matchEng.placeOrder(triggerAndAddToCommandQueueOrder);

        Order incoming = Order.createStandardOrder(100, 2, BUY, LIMIT, IOC, 96, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{97},
                new long[]{4},
                new long[]{1},
                new long[]{90},
                new long[]{10},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(3, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));
        checkEventRejection(incoming, 0, 3);
        checkEventTrade(incoming, 1, 1, 94, 1);
        checkEventTrade(incoming, 2, 2, 96, 1);
        checkEventTrade(incoming, 3, 3, 96, 5);
        checkEventTrade(incoming, 4, 4, 96, 5);
        checkEventTrade(incoming, 5, 5, 96, 5);

        assertThat(triggerAndAddToCommandQueueOrder.matcherTradeEvents.size(), is(1));
        checkEventTrade(triggerAndAddToCommandQueueOrder, 0, 6, 97, 1);
    }

    @Test
    void shouldMatchSellGTCAndTriggerStopOrder() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 106, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 104, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, BUY, STOP_MARKET, IOC, 105, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 105, 105, 5));
        matchEng.placeOrder(Order.createStopOrder(5, 1, BUY, STOP_LIMIT, GTC, 105, 104, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, BUY, STOP_LIMIT, GTC, 105, 103, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, SELL, STOP_LIMIT, GTC, 105, 110, 10));

        Order triggerAndAddToCommandQueueOrder = Order.createStopOrder(8, 2, SELL, STOP_MARKET, GTC, 105, 0, 1);
        matchEng.placeOrder(triggerAndAddToCommandQueueOrder);

        Order incoming = Order.createStandardOrder(100, 2, SELL, LIMIT, GTC, 104, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{104, 110},
                new long[]{3, 10},
                new long[]{1, 1},
                new long[]{103},
                new long[]{4},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(3, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(5));
        checkEventTrade(incoming, 0, 1, 106, 1);
        checkEventTrade(incoming, 1, 2, 104, 1);
        checkEventTrade(incoming, 2, 3, 104, 5);
        checkEventTrade(incoming, 3, 4, 104, 5);
        checkEventTrade(incoming, 4, 5, 104, 5);

        assertThat(triggerAndAddToCommandQueueOrder.matcherTradeEvents.size(), is(1));
        checkEventTrade(triggerAndAddToCommandQueueOrder, 0, 6, 103, 1);
    }

    @Test
    void shouldMatchSellIOCAndTriggerStopOrder() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 106, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 104, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, BUY, STOP_MARKET, IOC, 105, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 105, 105, 5));
        matchEng.placeOrder(Order.createStopOrder(5, 1, BUY, STOP_LIMIT, GTC, 105, 104, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, BUY, STOP_LIMIT, GTC, 105, 103, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, SELL, STOP_LIMIT, GTC, 105, 110, 10));

        Order triggerAndAddToCommandQueueOrder = Order.createStopOrder(8, 2, SELL, STOP_MARKET, GTC, 105, 0, 1);
        matchEng.placeOrder(triggerAndAddToCommandQueueOrder);

        Order incoming = Order.createStandardOrder(100, 2, SELL, LIMIT, IOC, 104, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{110},
                new long[]{10},
                new long[]{1},
                new long[]{103},
                new long[]{4},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(3, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));
        checkEventRejection(incoming, 0, 3);
        checkEventTrade(incoming, 1, 1, 106, 1);
        checkEventTrade(incoming, 2, 2, 104, 1);
        checkEventTrade(incoming, 3, 3, 104, 5);
        checkEventTrade(incoming, 4, 4, 104, 5);
        checkEventTrade(incoming, 5, 5, 104, 5);

        assertThat(triggerAndAddToCommandQueueOrder.matcherTradeEvents.size(), is(1));
        checkEventTrade(triggerAndAddToCommandQueueOrder, 0, 6, 103, 1);
    }

    @Test
    void shouldMatchBuyGTCAndTriggerStopIcebergOrder() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 95, 0, 5));
        matchEng.placeOrder(Order.createStopIcebergOrder(4, 1, SELL, STOP_LIMIT,95, 95, 1000, 2));
        matchEng.placeOrder(Order.createStopOrder(5, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10));

        Order triggerAndAddToCommandQueueOrder = Order.createStopOrder(8, 2, BUY, STOP_LIMIT, GTC, 95, 100, 5);
        matchEng.placeOrder(triggerAndAddToCommandQueueOrder);

        Order incoming = Order.createStandardOrder(100, 2, BUY, LIMIT, GTC, 96, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{95, 96, 97},
                new long[]{982, 5, 5},
                new long[]{1, 1, 1},
                new long[]{90},
                new long[]{10},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(4));
        checkEventTrade(incoming, 0, 1, 94, 1);
        checkEventTrade(incoming, 1, 2, 96, 1);
        checkEventTrade(incoming, 2, 3, 96, 5);
        checkEventTrade(incoming, 3, 4, 96, 13);

        assertThat(triggerAndAddToCommandQueueOrder.matcherTradeEvents.size(), is(3));
        checkEventTrade(triggerAndAddToCommandQueueOrder, 0, 4, 95, 2);
        checkEventTrade(triggerAndAddToCommandQueueOrder, 1, 4, 95, 2);
        checkEventTrade(triggerAndAddToCommandQueueOrder, 2, 4, 95, 1);
    }

    @Test
    void shouldAddStopOrderToOrderBookWhenLastPriceEqualStopPrice() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1));
        matchEng.placeOrder(Order.createStandardOrder(3, 2, BUY, LIMIT, GTC, 94, 1));

        Order incoming = Order.createStopOrder(4, 2, BUY, STOP_LIMIT, GTC, 94, 95, 5);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{96},
                new long[]{1},
                new long[]{1},
                new long[]{95},
                new long[]{5},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(5, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(0));
    }

    @Test
    void shouldMatchStopOrderImmediatelyWhenLastPriceEqualStopPrice() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1));
        matchEng.placeOrder(Order.createStandardOrder(3, 2, BUY, LIMIT, GTC, 94, 1));

        Order incoming = Order.createStopOrder(4, 2, BUY, STOP_LIMIT, GTC, 94, 100, 5);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{100},
                new long[]{4},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(4, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventTrade(incoming, 0, 2, 96, 1);
    }

    @Test
    void shouldMatchFOKFromOrderBookAndStopBook() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 110, 10));
        matchEng.placeOrder(Order.createStopOrder(3, 1, BUY, STOP_LIMIT, GTC, 105, 105, 3));
        matchEng.placeOrder(Order.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 100, 100, 3));

        Order incoming = Order.createStandardOrder(10, 2, SELL, MARKET, FOK, 0, 25);
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

        assertThat(incoming.matcherTradeEvents.size(), is(4));
        checkEventTrade(incoming, 0, 2, 110, 10);
        checkEventTrade(incoming, 1, 1, 100, 10);
        checkEventTrade(incoming, 2, 3, 100, 3);
        checkEventTrade(incoming, 3, 4, 100, 2);
    }

    @Test
    void shouldTriggerStopBuyFromToLowAndRespectFIFO() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, SELL, LIMIT, GTC, 93, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 94, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 96, 90, 10));
        matchEng.placeOrder(Order.createStopOrder(5, 1, SELL, STOP_MARKET, IOC, 95, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 95, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5));
        matchEng.placeOrder(Order.createStopOrder(8, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5));
        matchEng.placeOrder(Order.createStopOrder(9, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10));

        Order incoming = Order.createStandardOrder(100, 2, BUY, LIMIT, GTC, 96, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{96, 97},
                new long[]{2, 5},
                new long[]{1, 1},
                new long[]{90},
                new long[]{20},
                new long[]{2}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));
        checkEventTrade(incoming, 0, 1, 93, 1);
        checkEventTrade(incoming, 1, 2, 96, 1);
        checkEventTrade(incoming, 2, 3, 96, 5);
        checkEventTrade(incoming, 3, 5, 96, 5);
        checkEventTrade(incoming, 4, 6, 96, 5);
        checkEventTrade(incoming, 5, 7, 96, 3);
    }


    @Test
    void shouldTriggerStopSellFromHighToLowAndRespectFIFO() {
        MatchEng matchEng = new MatchEng();

        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 93, 1));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 96, 1));

        matchEng.placeOrder(Order.createStopOrder(3, 1, BUY, STOP_MARKET, IOC, 94, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(4, 1, SELL, STOP_LIMIT, GTC, 96, 90, 10));
        matchEng.placeOrder(Order.createStopOrder(5, 1, BUY, STOP_MARKET, IOC, 95, 0, 5));
        matchEng.placeOrder(Order.createStopOrder(6, 1, BUY, STOP_LIMIT, GTC, 95, 95, 5));
        matchEng.placeOrder(Order.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 96, 5));
        matchEng.placeOrder(Order.createStopOrder(8, 1, BUY, STOP_LIMIT, GTC, 95, 97, 5));
        matchEng.placeOrder(Order.createStopOrder(9, 1, SELL, STOP_LIMIT, GTC, 95, 90, 10));

        Order incoming = Order.createStandardOrder(100, 2, SELL, LIMIT, GTC, 93, 20);
        matchEng.placeOrder(incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{90},
                new long[]{10},
                new long[]{1},
                new long[]{97},
                new long[]{2},
                new long[]{1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));
        checkEventTrade(incoming, 0, 2, 96, 1);
        checkEventTrade(incoming, 1, 1, 93, 1);
        checkEventTrade(incoming, 2, 5, 93, 5);
        checkEventTrade(incoming, 3, 6, 93, 5);
        checkEventTrade(incoming, 4, 7, 93, 5);
        checkEventTrade(incoming, 5, 8, 93, 3);
    }

}
