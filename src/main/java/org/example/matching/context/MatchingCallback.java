package org.example.matching.context;

import org.example.common.command.OrderCommand;
import org.example.order.Order;
import org.example.orderbook.DirectOrder;

/**
 * Callback interface used to handle actual trade execution
 * when a match is found between an incoming order and a resting order.
 *
 * <p>Typically used within the matching engine to apply custom trade logic,
 * update order state, generate trade events, and update the last traded price.</p>
 */
public interface MatchingCallback {

    /**
     * Executes a trade between an incoming order and a resting order.
     *
     * @param cmd         the original {@link OrderCommand} that triggered the match;
     *                    trade events will be recorded into this object.
     * @param incoming    the incoming taker order.
     * @param restingOrder the resting maker order from the order book.
     * @param tradeSize   the quantity to be matched between the two orders.
     */
    void performMatch(OrderCommand cmd, Order incoming, DirectOrder restingOrder, long tradeSize);

}
