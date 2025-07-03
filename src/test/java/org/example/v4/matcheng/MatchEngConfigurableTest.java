package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.common.command.CommandResultCode;
import org.example.v4.common.command.OrderCommand;
import org.example.v4.matching.MatchingStrategy;
import org.example.v4.order.Order;
import org.example.v4.order.TimeInForce;
import org.junit.jupiter.api.Test;

import static org.example.v4.common.command.CommandResultCode.SUCCESS;
import static org.example.v4.order.OrderSide.BUY;
import static org.example.v4.order.OrderSide.SELL;
import static org.example.v4.order.OrderType.LIMIT;
import static org.example.v4.order.OrderType.MARKET;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class MatchEngConfigurableTest extends BaseMatchEngTest {

    MatchEngImpl matchEng;

    static final long MAX_PRICE = 400000L;

    static final long UID_1 = 412;
    static final long UID_2 = 413;
    static final long UID_3 = 414;
    static final long UID_4 = 415;
    static final long UID_5 = 416;




    @Test
    public void shouldMatchIocFIFO() {
        matchEng = new MatchEngImpl(MatchingStrategy.FIFO);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(3));
        checkEventTrade(cmd, 0, 123,8L, 201000L, 28L);
        checkEventTrade(cmd, 1, 123, 9L, 201000L,32L);
        checkEventTrade(cmd, 2, 123, 100L, 201000L, 95L);

    }

    @Test
    public void shouldMatchIocFIFO_SkipSelfMatchOrders() {
        matchEng = new MatchEngImpl(MatchingStrategy.FIFO);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, UID_2, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(3));
        checkEventTrade(cmd, 0, 123, 8L, 201000L, 28L);
        checkEventTrade(cmd, 1, 123, 100L, 201000L, 100L);
        checkEventTrade(cmd, 2, 123,101L, 201000L, 27L);
    }


    @Test
    public void shouldMatchIocProRata() {
        matchEng = new MatchEngImpl(MatchingStrategy.PRO_RATA);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(6));
        checkEventTrade(cmd, 0, 123, 8L, 201000L, 16L); // PRO-Rata       28/262 * 155 = 16
        checkEventTrade(cmd, 1, 123, 9L, 201000L, 18L); // PRO-Rata       32/262 * 155 = 18
        checkEventTrade(cmd, 2, 123, 100L, 201000L, 59L); // PRO-Rata     100/262 * 155 = 59
        checkEventTrade(cmd, 3, 123, 101L, 201000L, 59L); // PRO-Rata     100/262 * 155 = 59
        checkEventTrade(cmd, 4, 123, 102L, 201000L, 1L); // PRO-Rata      2/262 * 155 = 1
        checkEventTrade(cmd, 5, 123,8L, 201000L, 2L); // FIFO Residual   155 - 16 - 18 - 59 -59 - 1 = 2
    }

    @Test
    public void shouldMatchIocProRata_FIFOException() {
        matchEng = new MatchEngImpl(MatchingStrategy.PRO_RATA);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 500);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(6));

        checkEventRejection(cmd, 0, 238);
        checkEventTrade(cmd, 1, 123, 8L, 201000L, 28L);
        checkEventTrade(cmd, 2, 123, 9L, 201000L, 32L);
        checkEventTrade(cmd, 3, 123, 100L, 201000L, 100L);
        checkEventTrade(cmd, 4, 123, 101L, 201000L, 100L);
        checkEventTrade(cmd, 5, 123, 102L, 201000L, 2L);
    }

    @Test
    public void shouldMatchIocProRata_SkipSelfMatchOrders() {
        matchEng = new MatchEngImpl(MatchingStrategy.PRO_RATA);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, UID_2, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(5));
        checkEventTrade(cmd, 0, 123, 8L, 201000L, 18L); // PRO-Rata       28/230 * 155 = 18
        checkEventTrade(cmd, 1, 123, 100L, 201000L, 67L); // PRO-Rata     100/230 * 155 = 67
        checkEventTrade(cmd, 2, 123, 101L, 201000L, 67L); // PRO-Rata     100/230 * 155 = 67
        checkEventTrade(cmd, 3, 123,102L, 201000L, 1L); // PRO-Rata      2/230 * 155 = 1
        checkEventTrade(cmd, 4, 123, 8L, 201000L, 2L); // FIFO Residual   155 - 18 - 67 - 67 - 1 = 2


    }


    @Test
    public void shouldMatchIocLMM() {
        matchEng = new MatchEngImpl(MatchingStrategy.LMM);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(5));
        checkEventTrade(cmd, 0, 123,8L, 201000L, 7L);
        checkEventTrade(cmd, 1, 123,9L, 201000L, 9L);
        checkEventTrade(cmd, 2, 123, 8L, 201000L, 21L);
        checkEventTrade(cmd, 3, 123,9L, 201000L, 23L);
        checkEventTrade(cmd, 4, 123,100L, 201000L, 95L);
    }

    @Test
    public void shouldMatchIocLMM_FIFOException() {
        matchEng = new MatchEngImpl(MatchingStrategy.LMM);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, -1, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 500);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(6));

        checkEventRejection(cmd, 0, 238);
        checkEventTrade(cmd, 1, 123,8L, 201000L, 28L);
        checkEventTrade(cmd, 2, 123,9L, 201000L, 32L);
        checkEventTrade(cmd, 3, 123,100L, 201000L, 100L);
        checkEventTrade(cmd, 4, 123,101L, 201000L, 100L);
        checkEventTrade(cmd, 5, 123,102L, 201000L, 2L);
    }

    @Test
    public void shouldMatchIocLMM_SkipSelfMatchOrders() {
        matchEng = new MatchEngImpl(MatchingStrategy.LMM);
        initOrderBook();

        OrderCommand cmd = OrderCommand.createStandardOrder(123, UID_2, BUY, MARKET, TimeInForce.IOC, MAX_PRICE, 155);
        processAndValidate(matchEng, cmd, SUCCESS);

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

        assertThat(cmd.extractEvents().size(), is(4));
        checkEventTrade(cmd, 0, 123,8L, 201000L, 7L);
        checkEventTrade(cmd, 1, 123,8L, 201000L, 21L);
        checkEventTrade(cmd, 2, 123,100L, 201000L, 100L);
        checkEventTrade(cmd, 3, 123,101L, 201000L, 27L);
    }


    private void initOrderBook() {
        processAndValidate(matchEng, OrderCommand.createStandardOrder(8, UID_1, SELL, LIMIT, TimeInForce.GTC, 201000L, 28L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(9, UID_2, SELL, LIMIT, TimeInForce.GTC, 201000L, 32L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(100, UID_3, SELL, LIMIT, TimeInForce.GTC, 201000L, 100L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(101, UID_4, SELL, LIMIT, TimeInForce.GTC, 201000L, 100L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(102, UID_5, SELL, LIMIT, TimeInForce.GTC, 201000L, 2L), SUCCESS);

        processAndValidate(matchEng, OrderCommand.createStandardOrder(4, UID_1, BUY, LIMIT, TimeInForce.GTC, 81593L, 40L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(5, UID_2, BUY, LIMIT, TimeInForce.GTC, 81590L, 20L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(6, UID_3, BUY, LIMIT, TimeInForce.GTC, 81590L, 1L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(7, UID_4, BUY, LIMIT, TimeInForce.GTC, 81200L, 20L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(11, UID_5, BUY, LIMIT, TimeInForce.GTC, 10000L, 12L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(12, UID_1, BUY, LIMIT, TimeInForce.GTC, 10000L, 1L), SUCCESS);
        processAndValidate(matchEng, OrderCommand.createStandardOrder(13, UID_2, BUY, LIMIT, TimeInForce.GTC, 9136L, 2L), SUCCESS);


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
