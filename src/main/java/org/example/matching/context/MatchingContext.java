package org.example.matching.context;

import org.example.common.command.OrderCommand;
import org.example.order.Order;
import org.example.orderbook.DirectOrder;
import org.example.orderbook.PriceLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * MatchingContext holds the temporary state for matching a single incoming order
 * against a price level in the order book.
 *
 * <p>It tracks relevant data such as the command, incoming order, current price level,
 * remaining liquidity, iceberg refills, and self-matching orders.</p>
 *
 * <p>Matching is performed via the {@link MatchingCallback}, which is provided externally
 * and invoked for each successful match.</p>
 */
public class MatchingContext {

    /**
     * Callback used to execute actual trade logic between taker and maker.
     */
    public final MatchingCallback matchingCallback;

    /**
     * The command that initiated this matching session.
     * Trade events are recorded into this object.
     */
    public OrderCommand cmd;

    /**
     * The incoming taker order being matched.
     */
    public Order incoming;

    /**
     * The current price level being matched against.
     */
    public PriceLevel priceLevel;

    /**
     * Remaining displayed liquidity in the current price level
     * (excluding the user’s own orders).
     */
    public long bucketRemaining;

    /**
     * List of orders that would result in a self-match.
     * These are not matched but may be reinserted later.
     */
    public List<Order> selfMatchOrders = new ArrayList<>();

    /**
     * List of newly refilled iceberg child orders generated during matching.
     * These will be inserted into the order book after matching completes.
     */
    public List<Order> refilledOrders = new ArrayList<>();

    /**
     * Constructs a new MatchingContext with the given matching callback.
     *
     * @param matchingCallback the callback used to perform actual trade execution.
     */
    public MatchingContext(MatchingCallback matchingCallback) {
        this.matchingCallback = matchingCallback;
    }

    /**
     * Initializes the matching context for a new incoming order.
     *
     * @param cmd the command being processed.
     * @param incoming the incoming order to match.
     */
    public void initContext(OrderCommand cmd, Order incoming) {
        this.cmd = cmd;
        this.incoming = incoming;
        this.selfMatchOrders.clear();
    }

    /**
     * Sets the current price level to match against and calculates
     * available displayed liquidity excluding the current user.
     *
     * @param priceLevel the price level to match against.
     */
    public void updatePriceLevel(PriceLevel priceLevel) {
        this.priceLevel = priceLevel;
        this.refilledOrders.clear();
        this.bucketRemaining = priceLevel.getDisplayedQuantityWithoutUser(incoming.userId);
    }

    /**
     * Executes a match against a single resting order.
     * Updates internal state and handles iceberg refill logic.
     *
     * @param resting the resting order from the book.
     * @param tradeSize the quantity to match.
     */
    public void performMatch(DirectOrder resting, long tradeSize) {
        this.bucketRemaining -= tradeSize;
        matchingCallback.performMatch(cmd, incoming, resting, tradeSize);

        if(resting.displayedQuantity == 0) {
            Order icebergChild = resting.createIcebergChild();
            if(icebergChild != null) {
                refilledOrders.add(icebergChild);
            }
        }
    }

}
