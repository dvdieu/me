package org.example.matcheng;

import org.example.common.L2MarketData;
import org.example.common.command.OrderCommand;
import org.junit.jupiter.api.Test;

import static org.example.common.command.CommandResultCode.SUCCESS;
import static org.example.order.OrderSide.BUY;
import static org.example.order.OrderSide.SELL;
import static org.example.order.OrderType.LIMIT;
import static org.example.order.OrderType.MARKET;
import static org.example.order.TimeInForce.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


class MatchEngTest extends BaseMatchEngTest {

    @Test
    void shouldMatchFully() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 110, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 5);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(1));
        checkEventTrade(cmd, 0, 3, 1, 100, 5);
    }

    @Test
    void shouldMatchGTCPartially() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(2, 2, SELL, LIMIT, GTC, 100, 10);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(1));
        checkEventTrade(cmd, 0, 2, 1, 100, 5);
    }

    @Test
    void shouldMatchIOCPartially() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(2, 2, SELL, MARKET, IOC, 100, 10);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(2));
        checkEventTrade(cmd, 0, 2, 1, 100, 5);
        checkEventRejection(cmd, 1, 5);
    }

    @Test
    void shouldNoMatchGTC() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 95, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(2, 2, SELL, LIMIT, GTC, 100, 10);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(0));
    }

    @Test
    void shouldNoMatchIOC() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 95, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(2, 2, SELL, MARKET, IOC, 100, 10);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(1));
        checkEventRejection(cmd, 0, 10);
    }

    @Test
    void shouldNoMatchFOK() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 110, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(3, 2, SELL, MARKET, FOK, 0, 25);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(1));
        checkEventRejection(cmd, 0, 25);
    }

    @Test
    void shouldMatchMultiplePriceLevels() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 101, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 5), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 9);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(2));
        checkEventTrade(cmd, 0,3, 1, 101, 5);
        checkEventTrade(cmd, 1, 3, 2, 100, 4);
    }

    @Test
    void shouldRefilledIcebergs() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createIcebergOrder(1, 1, BUY, 100, 10, 2), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 5), SUCCESS);

        OrderCommand incoming = OrderCommand.createStandardOrder(3, 2, SELL, LIMIT, GTC, 100, 10);
        processAndValidate(matchEng, incoming, SUCCESS);

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

        assertThat(incoming.extractEvents().size(), is(4));
        checkEventTrade(incoming, 0, 3,1, 100, 2);
        checkEventTrade(incoming, 1, 3, 2, 100, 5);
        checkEventTrade(incoming, 2, 3, 1, 100, 2);
        checkEventTrade(incoming, 3, 3, 1, 100, 1);
    }


    @Test
    public void shouldAddPostOnlyOrders() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(3, 2, SELL, LIMIT, GTC, 110, 10).postOnly();
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(0));
    }


    @Test
    public void shouldRejectPostOnlyOrders() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 100, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(3, 2, SELL, LIMIT, GTC, 90, 10).postOnly();
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(1));
        checkEventRejection(cmd, 0, 10);
    }

}
