package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.matching.MatchingStrategy;
import org.example.v4.order.Order;
import org.example.v4.order.TimeInForce;
import org.junit.jupiter.api.Test;

import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.LIMIT;
import static org.example.v4.order.OrderType.MARKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class MatchEngConfigurableTest extends BaseMatchEngTest {

    MatchEng matchEng;

    static final long MAX_PRICE = 400000L;

    static final int UID_1 = 412;
    static final int UID_2 = 413;
    static final int UID_3 = 414;
    static final int UID_4 = 415;
    static final int UID_5 = 416;




    @Test
    public void shouldMatchIocFIFO() {
        matchEng = new MatchEng(MatchingStrategy.FIFO);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{201000},
                new long[]{107},
                new long[]{3},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(3));
        checkEventTrade(incoming, 0, 8L, 201000L, 28L);
        checkEventTrade(incoming, 1, 9L, 201000L,32L);
        checkEventTrade(incoming, 2, 100L, 201000L, 95L);

    }

    @Test
    public void shouldMatchIocFIFO_SkipSelfMatchOrders() {
        matchEng = new MatchEng(MatchingStrategy.FIFO);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, UID_2, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{201000},
                new long[]{107},
                new long[]{3},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(3));
        checkEventTrade(incoming, 0, 8L, 201000L, 28L);
        checkEventTrade(incoming, 1, 100L, 201000L, 100L);
        checkEventTrade(incoming, 2, 101L, 201000L, 27L);
    }


    @Test
    public void shouldMatchIocProRata() {
        matchEng = new MatchEng(MatchingStrategy.PRO_RATA);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{201000},
                new long[]{107},
                new long[]{5},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));
        checkEventTrade(incoming, 0, 8L, 201000L, 16L); // PRO-Rata       28/262 * 155 = 16
        checkEventTrade(incoming, 1, 9L, 201000L, 18L); // PRO-Rata       32/262 * 155 = 18
        checkEventTrade(incoming, 2, 100L, 201000L, 59L); // PRO-Rata     100/262 * 155 = 59
        checkEventTrade(incoming, 3, 101L, 201000L, 59L); // PRO-Rata     100/262 * 155 = 59
        checkEventTrade(incoming, 4, 102L, 201000L, 1L); // PRO-Rata      2/262 * 155 = 1
        checkEventTrade(incoming, 5, 8L, 201000L, 2L); // FIFO Residual   155 - 16 - 18 - 59 -59 - 1 = 2
    }

    @Test
    public void shouldMatchIocProRata_FIFOException() {
        matchEng = new MatchEng(MatchingStrategy.PRO_RATA);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 500);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(238, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));

        checkEventRejection(incoming, 0, 238);
        checkEventTrade(incoming, 1, 8L, 201000L, 28L);
        checkEventTrade(incoming, 2, 9L, 201000L, 32L);
        checkEventTrade(incoming, 3, 100L, 201000L, 100L);
        checkEventTrade(incoming, 4, 101L, 201000L, 100L);
        checkEventTrade(incoming, 5, 102L, 201000L, 2L);
    }

    @Test
    public void shouldMatchIocProRata_SkipSelfMatchOrders() {
        matchEng = new MatchEng(MatchingStrategy.PRO_RATA);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, UID_2, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{201000},
                new long[]{107},
                new long[]{5},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(5));
        checkEventTrade(incoming, 0, 8L, 201000L, 18L); // PRO-Rata       28/230 * 155 = 18
        checkEventTrade(incoming, 1, 100L, 201000L, 67L); // PRO-Rata     100/230 * 155 = 67
        checkEventTrade(incoming, 2, 101L, 201000L, 67L); // PRO-Rata     100/230 * 155 = 67
        checkEventTrade(incoming, 3, 102L, 201000L, 1L); // PRO-Rata      2/230 * 155 = 1
        checkEventTrade(incoming, 4, 8L, 201000L, 2L); // FIFO Residual   155 - 18 - 67 - 67 - 1 = 2


    }


    @Test
    public void shouldMatchIocLMM() {
        matchEng = new MatchEng(MatchingStrategy.LMM);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{201000},
                new long[]{107},
                new long[]{3},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(5));
        checkEventTrade(incoming, 0, 8L, 201000L, 7L);
        checkEventTrade(incoming, 1, 9L, 201000L, 9L);
        checkEventTrade(incoming, 2, 8L, 201000L, 21L);
        checkEventTrade(incoming, 3, 9L, 201000L, 23L);
        checkEventTrade(incoming, 4, 100L, 201000L, 95L);
    }

    @Test
    public void shouldMatchIocLMM_FIFOException() {
        matchEng = new MatchEng(MatchingStrategy.LMM);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 500);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{},
                new long[]{},
                new long[]{},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(238, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(6));

        checkEventRejection(incoming, 0, 238);
        checkEventTrade(incoming, 1, 8L, 201000L, 28L);
        checkEventTrade(incoming, 2, 9L, 201000L, 32L);
        checkEventTrade(incoming, 3, 100L, 201000L, 100L);
        checkEventTrade(incoming, 4, 101L, 201000L, 100L);
        checkEventTrade(incoming, 5, 102L, 201000L, 2L);
    }

    @Test
    public void shouldMatchIocLMM_SkipSelfMatchOrders() {
        matchEng = new MatchEng(MatchingStrategy.LMM);
        initOrderBook();

        Order incoming = Order.createStandardOrder(123, UID_2, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{201000},
                new long[]{107},
                new long[]{3},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(4));
        checkEventTrade(incoming, 0, 8L, 201000L, 7L);
        checkEventTrade(incoming, 1, 8L, 201000L, 21L);
        checkEventTrade(incoming, 2, 100L, 201000L, 100L);
        checkEventTrade(incoming, 3, 101L, 201000L, 27L);
    }


    private void initOrderBook() {
        placeOrderAndValidate(matchEng, Order.createStandardOrder(8, UID_1, SELL, LIMIT, TimeInForce.GTC, 201000L, 28L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(9, UID_2, SELL, LIMIT, TimeInForce.GTC, 201000L, 32L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(100, UID_3, SELL, LIMIT, TimeInForce.GTC, 201000L, 100L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(101, UID_4, SELL, LIMIT, TimeInForce.GTC, 201000L, 100L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(102, UID_5, SELL, LIMIT, TimeInForce.GTC, 201000L, 2L));

        placeOrderAndValidate(matchEng, Order.createStandardOrder(4, UID_1, BUY, LIMIT, TimeInForce.GTC, 81593L, 40L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(5, UID_2, BUY, LIMIT, TimeInForce.GTC, 81590L, 20L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(6, UID_3, BUY, LIMIT, TimeInForce.GTC, 81590L, 1L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(7, UID_4, BUY, LIMIT, TimeInForce.GTC, 81200L, 20L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(11, UID_5, BUY, LIMIT, TimeInForce.GTC, 10000L, 12L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(12, UID_1, BUY, LIMIT, TimeInForce.GTC, 10000L, 1L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(13, UID_2, BUY, LIMIT, TimeInForce.GTC, 9136L, 2L));


        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expectedState = new L2MarketData(
                new long[]{201000},
                new long[]{262},
                new long[]{5},
                new long[]{81593, 81590, 81200, 10000, 9136},
                new long[]{40, 21, 20, 13, 2},
                new long[]{1, 2, 1, 2, 1}
        );

        assertEquals(expectedState, snapshot);
    }

}
