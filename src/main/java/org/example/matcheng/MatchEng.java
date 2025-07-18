package org.example.matcheng;

import org.example.common.L2MarketData;
import org.example.common.command.CommandResultCode;
import org.example.common.command.OrderCommand;
import org.example.order.Order;

/**
 * MatchEng defines the core functionalities of a matching engine,
 * including placing and canceling orders, accessing order and market data,
 * and verifying internal consistency.
 */
public interface MatchEng {

    /**
     * Places a new order into the matching engine.
     *
     * @param cmd the order command containing order details such as type, quantity, price, etc.
     * @return the result code indicating whether the order was accepted, rejected, or partially processed.
     */
    CommandResultCode placeOrder(OrderCommand cmd);

    /**
     * Cancels an existing order from the matching engine.
     *
     * @param cmd the cancel command including the orderId and user verification.
     * @return the result code indicating whether the cancellation was successful or failed.
     */
    CommandResultCode cancelOrder(OrderCommand cmd);

    /**
     * Retrieves an order by its ID.
     *
     * @param orderId the unique identifier of the order.
     * @return the corresponding Order object if found; otherwise, null.
     */
    Order getOrderById(long orderId);

    /**
     * Returns the current Level 2 market data snapshot.
     *
     * @return an {@link L2MarketData} object representing the current state of the order book.
     */
    L2MarketData getL2MarketData();

    /**
     * Validates the internal consistency of the matching engine.
     * Used primarily for testing and debugging purposes.
     *
     * @throws IllegalStateException if any inconsistency or corruption is detected.
     */
    void validateInternalState();

    /**
     * Dispatches the provided command to the appropriate handler (e.g., place or cancel).
     *
     * @param matchEng the {@link MatchEng} instance to operate on.
     * @param cmd the command to process.
     * @return the result code from the corresponding command handler.
     */
    static CommandResultCode processCommand(final MatchEng matchEng, final OrderCommand cmd) {
        return switch (cmd.command) {
            case CANCEL_ORDER -> matchEng.cancelOrder(cmd);
            case PLACE_ORDER -> matchEng.placeOrder(cmd);
        };
    }
}
