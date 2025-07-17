package org.example.matcheng;

import org.example.common.L2MarketData;
import org.example.common.command.OrderCommand;
import org.junit.jupiter.api.Test;

import static org.example.common.command.CommandResultCode.SUCCESS;
import static org.example.order.OrderSide.BUY;
import static org.example.order.OrderSide.SELL;
import static org.example.order.OrderType.*;
import static org.example.order.TimeInForce.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


class MatchEngStopOrderTest extends BaseMatchEngTest {

    @Test
    void shouldMatchBuyGTCAndTriggerStopOrder() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 95, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, SELL, STOP_LIMIT, GTC, 95, 95, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10), SUCCESS);

        OrderCommand triggerAndAddToCommandQueueOrder = OrderCommand.createStopOrder(8, 2, BUY, STOP_LIMIT, GTC, 95, 100, 1);
        processAndValidate(matchEng, triggerAndAddToCommandQueueOrder, SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, BUY, LIMIT, GTC, 96, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(6));
        checkEventTrade(cmd, 0, 100,1, 94, 1);
        checkEventTrade(cmd, 1, 100, 2, 96, 1);
        checkEventTrade(cmd, 2, 100, 3, 96, 5);
        checkEventTrade(cmd, 3, 100, 4, 96, 5);
        checkEventTrade(cmd, 4, 100, 5, 96, 5);
        checkEventTrade(cmd, 5, 8, 6, 97, 1);
    }

    @Test
    void shouldMatchBuyIOCAndTriggerStopOrder() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 95, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, SELL, STOP_LIMIT, GTC, 95, 95, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10), SUCCESS);

        OrderCommand triggerAndAddToCommandQueueOrder = OrderCommand.createStopOrder(8, 2, BUY, STOP_LIMIT, GTC, 95, 100, 1);
        processAndValidate(matchEng, triggerAndAddToCommandQueueOrder, SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, BUY, LIMIT, IOC, 96, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(7));
        checkEventTrade(cmd, 0, 100, 1, 94, 1);
        checkEventTrade(cmd, 1, 100, 2, 96, 1);
        checkEventTrade(cmd, 2, 100, 3, 96, 5);
        checkEventTrade(cmd, 3, 100, 4, 96, 5);
        checkEventTrade(cmd, 4, 100, 5, 96, 5);
        checkEventRejection(cmd, 5, 3);
        checkEventTrade(cmd, 6, 8, 6, 97, 1);
    }

    @Test
    void shouldMatchSellGTCAndTriggerStopOrder() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 106, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 104, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, BUY, STOP_MARKET, IOC, 105, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 105, 105, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, BUY, STOP_LIMIT, GTC, 105, 104, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, BUY, STOP_LIMIT, GTC, 105, 103, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, SELL, STOP_LIMIT, GTC, 105, 110, 10), SUCCESS);

        OrderCommand triggerAndAddToCommandQueueOrder = OrderCommand.createStopOrder(8, 2, SELL, STOP_MARKET, GTC, 105, 0, 1);
        processAndValidate(matchEng, triggerAndAddToCommandQueueOrder, SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, SELL, LIMIT, GTC, 104, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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
        assertThat(cmd.extractEvents().size(), is(6));
        checkEventTrade(cmd, 0, 100,1, 106, 1);
        checkEventTrade(cmd, 1, 100,2, 104, 1);
        checkEventTrade(cmd, 2, 100,3, 104, 5);
        checkEventTrade(cmd, 3, 100,4, 104, 5);
        checkEventTrade(cmd, 4, 100,5, 104, 5);
        checkEventTrade(cmd, 5, 8, 6, 103, 1);
    }

    @Test
    void shouldMatchSellIOCAndTriggerStopOrder() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 106, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 104, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, BUY, STOP_MARKET, IOC, 105, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 105, 105, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, BUY, STOP_LIMIT, GTC, 105, 104, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, BUY, STOP_LIMIT, GTC, 105, 103, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, SELL, STOP_LIMIT, GTC, 105, 110, 10), SUCCESS);

        OrderCommand triggerAndAddToCommandQueueOrder = OrderCommand.createStopOrder(8, 2, SELL, STOP_MARKET, GTC, 105, 0, 1);
        processAndValidate(matchEng, triggerAndAddToCommandQueueOrder, SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, SELL, LIMIT, IOC, 104, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(7));
        checkEventTrade(cmd, 0, 100, 1, 106, 1);
        checkEventTrade(cmd, 1, 100,2, 104, 1);
        checkEventTrade(cmd, 2, 100,3, 104, 5);
        checkEventTrade(cmd, 3, 100,4, 104, 5);
        checkEventTrade(cmd, 4, 100,5, 104, 5);
        checkEventRejection(cmd, 5, 3);
        checkEventTrade(cmd, 6, 8, 6, 103, 1);
    }

    @Test
    void shouldMatchBuyGTCAndTriggerStopIcebergOrder() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 95, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopIcebergOrder(4, 1, SELL, STOP_LIMIT,95, 95, 1000, 2), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10), SUCCESS);

        OrderCommand triggerAndAddToCommandQueueOrder = OrderCommand.createStopOrder(8, 2, BUY, STOP_LIMIT, GTC, 95, 100, 5);
        processAndValidate(matchEng, triggerAndAddToCommandQueueOrder, SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, BUY, LIMIT, GTC, 96, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(7));
        checkEventTrade(cmd, 0, 100, 1, 94, 1);
        checkEventTrade(cmd, 1, 100, 2, 96, 1);
        checkEventTrade(cmd, 2, 100, 3, 96, 5);
        checkEventTrade(cmd, 3, 100, 4, 96, 13);
        checkEventTrade(cmd, 4, 8, 4, 95, 2);
        checkEventTrade(cmd, 5, 8, 4, 95, 2);
        checkEventTrade(cmd, 6, 8, 4, 95, 1);
    }

    @Test
    void shouldAddStopOrderToOrderBookWhenLastPriceEqualStopPrice() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(3, 2, BUY, LIMIT, GTC, 94, 1), SUCCESS);

        OrderCommand cmd = OrderCommand.createStopOrder(4, 2, BUY, STOP_LIMIT, GTC, 94, 95, 5);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(0));
    }

    @Test
    void shouldMatchStopOrderImmediatelyWhenLastPriceEqualStopPrice() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, SELL, LIMIT, GTC, 94, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(3, 2, BUY, LIMIT, GTC, 94, 1), SUCCESS);

        OrderCommand cmd = OrderCommand.createStopOrder(4, 2, BUY, STOP_LIMIT, GTC, 94, 100, 5);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(1));
        checkEventTrade(cmd, 0, 4, 2, 96, 1);
    }

    @Test
    void shouldMatchFOKFromOrderBookAndStopBook() {
        MatchEngImpl matchEng = new MatchEngImpl();
        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 100, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 110, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, BUY, STOP_LIMIT, GTC, 105, 105, 3), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 100, 100, 3), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(10, 2, SELL, MARKET, FOK, 0, 25);
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

        assertThat(cmd.extractEvents().size(), is(4));
        checkEventTrade(cmd, 0, 10,2, 110, 10);
        checkEventTrade(cmd, 1, 10, 1, 100, 10);
        checkEventTrade(cmd, 2, 10, 3, 100, 3);
        checkEventTrade(cmd, 3, 10, 4, 100, 2);
    }

    @Test
    void shouldTriggerStopBuyFromToLowAndRespectFIFO() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, SELL, LIMIT, GTC, 93, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, SELL, LIMIT, GTC, 96, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, SELL, STOP_MARKET, IOC, 94, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, BUY, STOP_LIMIT, GTC, 96, 90, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, SELL, STOP_MARKET, IOC, 95, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, SELL, STOP_LIMIT, GTC, 95, 95, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, SELL, STOP_LIMIT, GTC, 95, 96, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(8, 1, SELL, STOP_LIMIT, GTC, 95, 97, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(9, 1, BUY, STOP_LIMIT, GTC, 95, 90, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, BUY, LIMIT, GTC, 96, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(6));
        checkEventTrade(cmd, 0, 100,1, 93, 1);
        checkEventTrade(cmd, 1, 100,2, 96, 1);
        checkEventTrade(cmd, 2, 100,3, 96, 5);
        checkEventTrade(cmd, 3, 100,5, 96, 5);
        checkEventTrade(cmd, 4, 100,6, 96, 5);
        checkEventTrade(cmd, 5, 100,7, 96, 3);
    }


    @Test
    void shouldTriggerStopSellFromHighToLowAndRespectFIFO() {
        MatchEngImpl matchEng = new MatchEngImpl();

        processAndValidate(matchEng, OrderCommand.createStandardOrder(1, 1, BUY, LIMIT, GTC, 93, 1), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(2, 1, BUY, LIMIT, GTC, 96, 1), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStopOrder(3, 1, BUY, STOP_MARKET, IOC, 94, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(4, 1, SELL, STOP_LIMIT, GTC, 96, 90, 10), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(5, 1, BUY, STOP_MARKET, IOC, 95, 0, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(6, 1, BUY, STOP_LIMIT, GTC, 95, 95, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(7, 1, BUY, STOP_LIMIT, GTC, 95, 96, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(8, 1, BUY, STOP_LIMIT, GTC, 95, 97, 5), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStopOrder(9, 1, SELL, STOP_LIMIT, GTC, 95, 90, 10), SUCCESS);

        OrderCommand cmd = OrderCommand.createStandardOrder(100, 2, SELL, LIMIT, GTC, 93, 20);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(7));
        checkEventTrade(cmd, 0, 100,2, 96, 1);
        checkEventTrade(cmd, 1, 100,1, 93, 1);
        checkEventTrade(cmd, 2, 100,5, 93, 5);
        checkEventTrade(cmd, 3, 100,6, 93, 5);
        checkEventTrade(cmd, 4, 100,7, 93, 5);
        checkEventTrade(cmd, 5, 100,8, 93, 3);
        checkEventRejection(cmd, 6,  3, 5);
    }

}
