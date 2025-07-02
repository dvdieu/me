package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.common.command.CommandResultCode;
import org.example.v4.common.command.OrderCommand;
import org.example.v4.order.Order;

public interface MatchEng {

    CommandResultCode placeOrder(OrderCommand cmd);

    CommandResultCode cancelOrder(OrderCommand cmd);

    Order getOrderById(long orderId);

    L2MarketData getL2MarketData();

    void validateInternalState();

    static CommandResultCode processCommand(final MatchEng matchEng, final OrderCommand cmd) {
        return switch (cmd.command) {
            case CANCEL_ORDER -> matchEng.cancelOrder(cmd);
            case PLACE_ORDER -> matchEng.placeOrder(cmd);
        };
    }
}
