package org.example.v4.matcheng;

import org.example.v4.order.Order;
import org.example.v4.common.L2MarketData;
import org.junit.jupiter.api.Test;

import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.LIMIT;
import static org.example.v4.order.OrderType.MARKET;
import static org.example.v4.order.TimeInForce.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


class MatchEngTest {

    @Test
    void shouldMatchFully() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, SELL, LIMIT, GTC, 110, 10));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 5);
        matchEng.placeOrder(incoming);

        assertEquals(0, incoming.remainingQuantity);

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
    }

    @Test
    void shouldMatchGTCPartially() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(2, 2, SELL, LIMIT, GTC, 100, 10);
        matchEng.placeOrder(incoming);

        assertEquals(5, incoming.remainingQuantity);

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
    }

    @Test
    void shouldMatchIOCPartially() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(2, 2, SELL, MARKET, IOC, 100, 10);
        matchEng.placeOrder(incoming);

        assertEquals(5, incoming.remainingQuantity);

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
    }

    @Test
    void shouldNoMatchGTC() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 95, 10));

        Order incoming = Order.createStandardOrder(2, 2, SELL, LIMIT, GTC, 100, 10);
        matchEng.placeOrder(incoming);

        assertEquals(10, incoming.remainingQuantity);

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
    }

    @Test
    void shouldNoMatchIOC() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 95, 10));

        Order incoming = Order.createStandardOrder(2, 2, SELL, MARKET, IOC, 100, 10);
        matchEng.placeOrder(incoming);

        assertEquals(10, incoming.remainingQuantity);

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
    }

    @Test
    void shouldNoMatchFOK() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 110, 10));

        Order incoming = Order.createStandardOrder(3, 2, SELL, MARKET, FOK, 0, 21);
        matchEng.placeOrder(incoming);

        assertEquals(21, incoming.remainingQuantity);

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
    }

    @Test
    void shouldMatchMultiplePriceLevels() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createStandardOrder(1, 1, BUY, LIMIT, GTC, 101, 5));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 9);
        matchEng.placeOrder(incoming);

        assertEquals(0, incoming.remainingQuantity);

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
    }

    @Test
    void shouldRefilledIcebergs() {
        MatchEng matchEng = new MatchEng();
        matchEng.placeOrder(Order.createIcebergOrder(1, 1, BUY, 100, 10, 2));
        matchEng.placeOrder(Order.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 5));

        Order incoming = Order.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 10);
        matchEng.placeOrder(incoming);

        assertEquals(0, incoming.remainingQuantity);

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
    }

}
