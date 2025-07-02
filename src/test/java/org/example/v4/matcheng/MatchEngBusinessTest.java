package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.order.Order;
import org.junit.jupiter.api.Test;

import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.*;
import static org.example.v4.order.TimeInForce.GTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class MatchEngBusinessTest extends BaseMatchEngTest {

    final int USER_BUY = 1;
    final int USER_SELL = 2;


    @Test
    void shouldMatchBuyOrderAndTriggerStopBook() {

        MatchEng matchEng = new MatchEng();
        placeOrderAndValidate(matchEng, Order.createStandardOrder(100, USER_BUY, BUY, LIMIT, GTC, 99, 1));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(101, USER_SELL, SELL, LIMIT, GTC, 99, 1));

        placeOrderAndValidate(matchEng, Order.createStandardOrder(102, USER_BUY, BUY, LIMIT, GTC, 98, 13));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(103, USER_BUY, BUY, LIMIT, GTC, 97, 14));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(104, USER_BUY, BUY, LIMIT, GTC, 96, 10));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(105, USER_BUY, BUY, LIMIT, GTC, 95, 18));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(106, USER_BUY, BUY, LIMIT, GTC, 94, 7));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(107, USER_BUY, BUY, LIMIT, GTC, 93, 12));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(108, USER_BUY, BUY, LIMIT, GTC, 92, 9));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(109, USER_BUY, BUY, LIMIT, GTC, 91, 10));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(110, USER_BUY, BUY, LIMIT, GTC, 90, 8));

        placeOrderAndValidate(matchEng, Order.createStandardOrder(111, USER_SELL, SELL, LIMIT, GTC, 100, 10));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(112, USER_SELL, SELL, LIMIT, GTC, 101, 12));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(113, USER_SELL, SELL, LIMIT, GTC, 102, 8));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(114, USER_SELL, SELL, LIMIT, GTC, 103, 15));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(115, USER_SELL, SELL, LIMIT, GTC, 104, 10));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(116, USER_SELL, SELL, LIMIT, GTC, 105, 20));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(117, USER_SELL, SELL, LIMIT, GTC, 106, 5));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(118, USER_SELL, SELL, LIMIT, GTC, 107, 6));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(119, USER_SELL, SELL, LIMIT, GTC, 108, 12));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(120, USER_SELL, SELL, LIMIT, GTC, 109, 7));
        placeOrderAndValidate(matchEng, Order.createStandardOrder(121, USER_SELL, SELL, LIMIT, GTC, 110, 9));

        placeOrderAndValidate(matchEng, Order.createStopOrder(1, USER_SELL, SELL, STOP_LIMIT, GTC, 103, 99, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(2, USER_BUY, BUY, STOP_LIMIT, GTC, 101, 102, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(3, USER_BUY, BUY, STOP_LIMIT, GTC, 97, 98, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(4, USER_SELL, SELL, STOP_LIMIT, GTC, 97, 102, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(5, USER_BUY, BUY, STOP_LIMIT, GTC, 103, 104, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(6, USER_BUY, BUY, STOP_MARKET, GTC, 97, 0, 2));
        placeOrderAndValidate(matchEng, Order.createStopOrder(7, USER_SELL, SELL, STOP_LIMIT, GTC, 98, 97, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(8, USER_BUY, BUY, STOP_LIMIT, GTC, 101, 98, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(9, USER_SELL, SELL, STOP_LIMIT, GTC, 101, 97, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(10, USER_SELL, SELL, STOP_MARKET, GTC, 95, 0, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(11, USER_BUY, BUY, STOP_LIMIT, GTC, 97, 100, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(12, USER_SELL, SELL, STOP_LIMIT, GTC, 105, 100, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(13, USER_BUY, BUY, STOP_LIMIT, GTC, 105, 102, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(14, USER_SELL, SELL, STOP_LIMIT, GTC, 101, 103, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(15, USER_SELL, SELL, STOP_MARKET, GTC, 101, 0, 2));
        placeOrderAndValidate(matchEng, Order.createStopOrder(16, USER_BUY, BUY, STOP_MARKET, GTC, 101, 0, 2));
        placeOrderAndValidate(matchEng, Order.createStopOrder(17, USER_SELL, SELL, STOP_LIMIT, GTC, 95, 98, 2));
        placeOrderAndValidate(matchEng, Order.createStopOrder(18, USER_SELL, SELL, STOP_LIMIT, GTC, 95, 97, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(19, USER_BUY, BUY, STOP_LIMIT, GTC, 97, 98, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(20, USER_SELL, SELL, STOP_MARKET, GTC, 96, 0, 3));
        placeOrderAndValidate(matchEng, Order.createStopOrder(21, USER_SELL, SELL, STOP_MARKET, GTC, 102, 0, 4));

        Order incoming = Order.createStandardOrder(1000, -1, BUY, MARKET, GTC, 0, 50);
        incoming.stopAfterFirstCommand = true;
        placeOrderAndValidate(matchEng, incoming);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{103, 104, 105, 106, 107, 108, 109, 110},
                new long[]{  7,  10,  20,   5,   6,  12,   7,   9},
                new long[]{  2,   1,   1,   1,   1,   1,   1,   1},

                new long[]{98, 97, 96, 95, 94, 93, 92, 91, 90},
                new long[]{16, 14, 10, 18,  7, 12,  9, 10,  8},
                new long[]{ 2,  1,  1,  1,  1,  1,  1,  1,  1}
        );

        assertEquals(expected, snapshot);
        assertEquals(0, incoming.remainingQuantity);

        assertThat(incoming.matcherTradeEvents.size(), is(7));
        checkEventTrade(incoming, 0, 111, 100, 10);
        checkEventTrade(incoming, 1, 112, 101, 12);
        checkEventTrade(incoming, 2, 9, 101, 3);
        checkEventTrade(incoming, 3, 15, 101, 2);
        checkEventTrade(incoming, 4, 113, 102, 8);
        checkEventTrade(incoming, 5, 21, 102, 4);
        checkEventTrade(incoming, 6, 114, 103, 11);
    }

}
