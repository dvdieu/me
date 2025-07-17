package org.example.matcheng;

import org.example.common.L2MarketData;
import org.example.common.command.CommandResultCode;
import org.example.common.command.OrderCommand;
import org.example.order.Order;

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
