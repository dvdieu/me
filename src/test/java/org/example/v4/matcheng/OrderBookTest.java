package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.common.L2MarketDataHelper;
import org.example.v4.order.Order;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.LIMIT;
import static org.example.v4.order.OrderType.MARKET;
import static org.example.v4.order.TimeInForce.GTC;
import static org.example.v4.order.TimeInForce.IOC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class OrderBookTest extends BaseMatchEngTest {

    private L2MarketDataHelper expectedState;

    private final MatchEng matchEng = new MatchEng();
    static final long MAX_PRICE = 400000L;

    static final int UID_1 = 412;
    static final int UID_2 = 413;


    @BeforeEach
    public void before() {
        placeOrderAndValidate(matchEng, Order.createStandardOrder(1, UID_1, SELL, LIMIT, GTC, 81600L, 100L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(2, UID_1, SELL, LIMIT, GTC, 81599L, 50L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(3, UID_1, SELL, LIMIT, GTC, 81599L, 25L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(8, UID_1, SELL, LIMIT, GTC, 201000L, 28L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(9, UID_1, SELL, LIMIT, GTC, 201000L, 32L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(10, UID_1, SELL, LIMIT, GTC, 200954L, 10L));

        placeOrderAndValidate(matchEng, Order.createStandardOrder(4, UID_1, BUY, LIMIT, GTC, 81593L, 40L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(5, UID_1, BUY, LIMIT, GTC, 81590L, 20L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(6, UID_1, BUY, LIMIT, GTC, 81590L, 1L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(7, UID_1, BUY, LIMIT, GTC, 81200L, 20L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(11, UID_1, BUY, LIMIT, GTC, 10000L, 12L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(12, UID_1, BUY, LIMIT, GTC, 10000L, 1L));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(13, UID_1, BUY, LIMIT, GTC, 9136L, 2L));

        expectedState = new L2MarketDataHelper(
                new L2MarketData(
                        new long[]{81599, 81600, 200954, 201000},
                        new long[]{75, 100, 10, 60},
                        new long[]{2, 1, 1, 2},
                        new long[]{81593, 81590, 81200, 10000, 9136},
                        new long[]{40, 21, 20, 13, 2},
                        new long[]{1, 2, 1, 2, 1}
                )
        );

        L2MarketData snapshot = matchEng.getL2MarketData();
        assertEquals(expectedState.build(), snapshot);
    }

    /**
     * In the end of each test remove all orders by sending market orders wit proper size.
     * Check order book is empty.
     */
    @AfterEach
    public void after() {
        clearOrderBook();
    }

    void clearOrderBook() {
        L2MarketData snapshot = matchEng.getL2MarketData();

        // match all asks
        long askSum = Arrays.stream(snapshot.askVolumes).sum();
        placeOrderAndValidate(matchEng, Order.createStandardOrder(100000000000L, -1, BUY, MARKET, IOC, 0, askSum));

        // match all bids
        long bidSum = Arrays.stream(snapshot.bidVolumes).sum();
        placeOrderAndValidate(matchEng, Order.createStandardOrder(100000000000L, -2, SELL, MARKET, IOC, 0, bidSum));

        assertThat(matchEng.getL2MarketData().askSize, is(0));
        assertThat(matchEng.getL2MarketData().bidSize, is(0));
    }

    // ------------------------ TESTS WITHOUT MATCHING -----------------------

    /**
     * Just place few GTC orders
     */
    @Test
    public void shouldAddGtcOrders() {
        placeOrderAndValidate(matchEng, Order.createStandardOrder(93, UID_1, SELL, LIMIT, GTC, 81598, 1));
        expectedState.insertAsk(0, 81598, 1);

        placeOrderAndValidate(matchEng, Order.createStandardOrder(94, UID_1, BUY, LIMIT, GTC, 81594, 9_000_000_000L));
        expectedState.insertBid(0, 81594, 9_000_000_000L);

        L2MarketData snapshot = matchEng.getL2MarketData();
        assertEquals(expectedState.build(), snapshot);

        placeOrderAndValidate(matchEng, Order.createStandardOrder(95, UID_1, SELL, LIMIT, GTC, 130000, 13_000_000_000L));
        expectedState.insertAsk(3, 130000, 13_000_000_000L);

        placeOrderAndValidate(matchEng, Order.createStandardOrder(96, UID_1, BUY, LIMIT, GTC, 1000, 4));
        expectedState.insertBid(6, 1000, 4);

        snapshot = matchEng.getL2MarketData();
        assertEquals(expectedState.build(), snapshot);
    }

    /**
     * Ignore order with duplicate orderId
     */
    @Test
    public void shouldIgnoredDuplicateOrder() {
        Order incoming = Order.createStandardOrder(1, UID_1, SELL, LIMIT, GTC, 81600, 100);
        placeOrderAndValidate(matchEng, incoming);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
    }

    @Test
    public void shouldRemoveBidOrder() {
        // remove bid order
        Order order = cancelOrderAndValidate(matchEng, 5);

        expectedState.setBidVolume(1, 1).decrementBidOrdersNum(1);
        assertEquals(expectedState.build(), matchEng.getL2MarketData());

        assertThat(order.matcherTradeEvents.size(), is(1));
        checkEventReduce(order, 0, 20L);
    }

    @Test
    public void shouldRemoveAskOrder() {
        // remove ask order
        Order order = cancelOrderAndValidate(matchEng, 2);

        expectedState.setAskVolume(0, 25).decrementAskOrdersNum(0);
        assertEquals(expectedState.build(), matchEng.getL2MarketData());

        assertThat(order.matcherTradeEvents.size(), is(1));
        checkEventReduce(order, 0, 50L);
    }

    @Test
    public void shouldRemoveOrderAndEmptyBucket() {
        Order order2 = cancelOrderAndValidate(matchEng, 2);
        assertThat(order2.matcherTradeEvents.size(), is(1));
        checkEventReduce(order2, 0, 50L);

        Order order3 = cancelOrderAndValidate(matchEng, 3);
        assertThat(order3.matcherTradeEvents.size(), is(1));
        checkEventReduce(order3, 0, 25L);

        assertEquals(expectedState.removeAsk(0).build(), matchEng.getL2MarketData());
    }

    // ------------------------ MATCHING TESTS -----------------------

    @Test
    public void shouldMatchIocOrderPartialBBO() {
        // size=10
        Order incoming = Order.createStandardOrder(123, UID_2, SELL, MARKET, IOC, 1, 10);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // best bid matched
        L2MarketData expected = expectedState.setBidVolume(0, 30).build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventTrade(incoming, 0, 4L, 81593, 10L);
    }

    @Test
    public void shouldMatchIocOrderWithTwoLimitOrdersPartial() {
        // size=41
        Order incoming = Order.createStandardOrder(123, UID_2, SELL, MARKET, IOC, 1, 41);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // bids matched
        L2MarketData expected = expectedState.removeBid(0).setBidVolume(0, 20).build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(2));
        checkEventTrade(incoming, 0, 4L, 81593, 40L);
        checkEventTrade(incoming, 1, 5L, 81590, 1L);

        // check orders are removed from map
        assertNull(matchEng.getOrderById(4L));
        assertNotNull(matchEng.getOrderById(5L));
    }

    @Test
    public void shouldMatchIocOrderFullLiquidity() {
        // size=175
        Order incoming = Order.createStandardOrder(123, UID_2, BUY, MARKET, IOC, MAX_PRICE, 175);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // all asks matched
        L2MarketData expected = expectedState.removeAsk(0).removeAsk(0).build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(3));
        checkEventTrade(incoming, 0, 2L, 81599L, 50L);
        checkEventTrade(incoming, 1, 3L, 81599L, 25L);
        checkEventTrade(incoming, 2, 1L, 81600L, 100L);

        // check orders are removed from map
        assertNull(matchEng.getOrderById(1L));
        assertNull(matchEng.getOrderById(2L));
        assertNull(matchEng.getOrderById(3L));
    }

    @Test
    public void shouldMatchIocOrderWithRejection() {
        // size=270
        Order incoming = Order.createStandardOrder(123, UID_2, BUY, MARKET, IOC, MAX_PRICE, 270);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // all asks matched
        L2MarketData expected = expectedState.removeAllAsks().build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(7));

        // 6 trades generated, first comes rejection with size=25 left unmatched
        checkEventRejection(incoming, 0, 25L);
    }

    // MARKETABLE GTC ORDERS

    @Test
    public void shouldFullyMatchMarketableGtcOrder() {
        // size=1
        Order incoming = Order.createStandardOrder(123, UID_2, BUY, LIMIT, GTC, 81599, 1);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // best ask partially matched
        L2MarketData expected = expectedState.setAskVolume(0, 74).build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(1));
        checkEventTrade(incoming, 0, 2L, 81599, 1L);
    }

    @Test
    public void shouldPartiallyMatchMarketableGtcOrderAndPlace() {
        // size=77
        Order incoming = Order.createStandardOrder(123, UID_2, BUY, LIMIT, GTC, 81599, 77);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // best asks fully matched, limit bid order placed
        L2MarketData expected = expectedState.removeAsk(0).insertBid(0, 81599, 2).build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(2));

        checkEventTrade(incoming, 0, 2L, 81599, 50L);
        checkEventTrade(incoming, 1, 3L, 81599, 25L);
    }

    @Test
    public void shouldFullyMatchMarketableGtcOrder2Prices() {
        // size=77
        Order incoming = Order.createStandardOrder(123, UID_2, BUY, LIMIT, GTC, 81600, 77);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // best asks fully matched, limit bid order placed
        L2MarketData expected = expectedState.removeAsk(0).setAskVolume(0, 98).build();
        assertEquals(expected, snapshot);

        assertThat(incoming.matcherTradeEvents.size(), is(3));

        checkEventTrade(incoming, 0, 2L, 81599, 50L);
        checkEventTrade(incoming, 1, 3L, 81599, 25L);
        checkEventTrade(incoming, 2, 1L, 81600, 2L);
    }

    @Test
    public void shouldFullyMatchMarketableGtcOrderWithAllLiquidity() {
        // size=1000
        Order incoming = Order.createStandardOrder(123, UID_2, BUY, LIMIT, GTC, 220000, 1000);
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        // best asks fully matched, limit bid order placed
        L2MarketData expected = expectedState.removeAllAsks().insertBid(0, 220000, 755).build();
        assertEquals(expected, snapshot);

        // trades only, rejection not generated for limit order
        assertThat(incoming.matcherTradeEvents.size(), is(6));

        checkEventTrade(incoming, 0, 2L, 81599, 50L);
        checkEventTrade(incoming, 1, 3L, 81599, 25L);
        checkEventTrade(incoming, 2, 1L, 81600, 100L);
        checkEventTrade(incoming, 3, 10L, 200954, 10L);
        checkEventTrade(incoming, 4, 8L, 201000, 28L);
        checkEventTrade(incoming, 5, 9L, 201000, 32L);
    }


}
