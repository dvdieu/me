package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.common.command.CommandResultCode;
import org.example.v4.common.command.OrderCommand;
import org.example.v4.order.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.example.v4.common.command.CommandResultCode.SUCCESS;
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

    MatchEngImpl matchEng;


    @BeforeEach
    public void before() {
        matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(100, USER_BUY, BUY, LIMIT, GTC, 99, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(101, USER_SELL, SELL, LIMIT, GTC, 99, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStandardOrder(102, USER_BUY, BUY, LIMIT, GTC, 98, 13), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(103, USER_BUY, BUY, LIMIT, GTC, 97, 14), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(104, USER_BUY, BUY, LIMIT, GTC, 96, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(105, USER_BUY, BUY, LIMIT, GTC, 95, 18), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(106, USER_BUY, BUY, LIMIT, GTC, 94, 7), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(107, USER_BUY, BUY, LIMIT, GTC, 93, 12), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(108, USER_BUY, BUY, LIMIT, GTC, 92, 9), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(109, USER_BUY, BUY, LIMIT, GTC, 91, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(110, USER_BUY, BUY, LIMIT, GTC, 90, 8), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStandardOrder(111, USER_SELL, SELL, LIMIT, GTC, 100, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(112, USER_SELL, SELL, LIMIT, GTC, 101, 12), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(113, USER_SELL, SELL, LIMIT, GTC, 102, 8), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(114, USER_SELL, SELL, LIMIT, GTC, 103, 15), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(115, USER_SELL, SELL, LIMIT, GTC, 104, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(116, USER_SELL, SELL, LIMIT, GTC, 105, 20), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(117, USER_SELL, SELL, LIMIT, GTC, 106, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(118, USER_SELL, SELL, LIMIT, GTC, 107, 6), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(119, USER_SELL, SELL, LIMIT, GTC, 108, 12), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(120, USER_SELL, SELL, LIMIT, GTC, 109, 7), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(121, USER_SELL, SELL, LIMIT, GTC, 110, 9), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(1, USER_SELL, SELL, STOP_LIMIT, GTC, 103, 99, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(2, USER_BUY, BUY, STOP_LIMIT, GTC, 101, 102, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(3, USER_BUY, BUY, STOP_LIMIT, GTC, 97, 98, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, USER_SELL, SELL, STOP_LIMIT, GTC, 97, 102, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, USER_BUY, BUY, STOP_LIMIT, GTC, 103, 104, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, USER_BUY, BUY, STOP_MARKET, GTC, 97, 0, 2), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, USER_SELL, SELL, STOP_LIMIT, GTC, 98, 97, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(8, USER_BUY, BUY, STOP_LIMIT, GTC, 101, 98, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(9, USER_SELL, SELL, STOP_LIMIT, GTC, 101, 97, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(10, USER_SELL, SELL, STOP_MARKET, GTC, 95, 0, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(11, USER_BUY, BUY, STOP_LIMIT, GTC, 97, 100, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(12, USER_SELL, SELL, STOP_LIMIT, GTC, 105, 100, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(13, USER_BUY, BUY, STOP_LIMIT, GTC, 105, 102, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(14, USER_SELL, SELL, STOP_LIMIT, GTC, 101, 103, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(15, USER_SELL, SELL, STOP_MARKET, GTC, 101, 0, 2), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(16, USER_BUY, BUY, STOP_MARKET, GTC, 101, 0, 2), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(17, USER_SELL, SELL, STOP_LIMIT, GTC, 95, 98, 2), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(18, USER_SELL, SELL, STOP_LIMIT, GTC, 95, 97, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(19, USER_BUY, BUY, STOP_LIMIT, GTC, 97, 98, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(20, USER_SELL, SELL, STOP_MARKET, GTC, 96, 0, 3), SUCCESS);
    }

    @Test
    void shouldMatchBuyOrderAndTriggerStopBook() {
        processAndValidate(matchEng, OrderCommand.createStopOrder(21, USER_SELL, SELL, STOP_MARKET, GTC, 102, 0, 4), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(1000, -1, BUY, MARKET, GTC, 0, 50);
        cmd.stopAfterFirstCommand = true;
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(7));
        checkEventTrade(cmd, 0, 1000,111, 100, 10);
        checkEventTrade(cmd, 1, 1000,112, 101, 12);
        checkEventTrade(cmd, 2, 1000,9, 101, 3);
        checkEventTrade(cmd, 3, 1000,15, 101, 2);
        checkEventTrade(cmd, 4, 1000,113, 102, 8);
        checkEventTrade(cmd, 5, 1000,21, 102, 4);
        checkEventTrade(cmd, 6, 1000,114, 103, 11);
    }


    @Test
    void shouldMatchSellOrderAndTriggerStopBook() {
        processAndValidate(matchEng, OrderCommand.createStopOrder(21, USER_SELL, BUY, STOP_LIMIT, GTC, 98, 98, 4), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(1000, -1, SELL, MARKET, GTC, 0, 110);
        cmd.stopAfterFirstCommand = true;
        processAndValidate(matchEng, cmd, SUCCESS);

        L2MarketData snapshot = matchEng.getL2MarketData();
        L2MarketData expected = new L2MarketData(
                new long[]{97, 98, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110},
                new long[]{ 3,  2,  10,  12,  11,  15,  10,  20,   5,   6,  12,   7,   9},
                new long[]{ 1,  1,   1,   1,   2,   1,   1,   1,   1,   1,   1,   1,   1},

                new long[]{90},
                new long[]{6},
                new long[]{1}
        );

        assertEquals(expected, snapshot);

        assertThat(cmd.extractEvents().size(), is(14));
        checkEventTrade(cmd, 0, 1000,102, 98, 13);
        checkEventTrade(cmd, 1, 1000,21, 98, 4);
        checkEventTrade(cmd, 2, 1000,103, 97, 14);
        checkEventTrade(cmd, 3, 1000,3, 97, 3);
        checkEventTrade(cmd, 4, 1000,6, 97, 2);
        checkEventTrade(cmd, 5, 1000,11, 97, 3);
        checkEventTrade(cmd, 6, 1000,19, 97, 3);
        checkEventTrade(cmd, 7, 1000,104, 96, 10);
        checkEventTrade(cmd, 8, 1000,105, 95, 18);
        checkEventTrade(cmd, 9, 1000,106, 94, 7);
        checkEventTrade(cmd, 10, 1000,107, 93, 12);
        checkEventTrade(cmd, 11, 1000,108, 92, 9);
        checkEventTrade(cmd, 12, 1000,109, 91, 10);
        checkEventTrade(cmd, 13, 1000,110, 90, 2);
    }

}
