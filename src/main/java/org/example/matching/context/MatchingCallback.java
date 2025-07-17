package org.example.matching.context;

import org.example.common.command.OrderCommand;
import org.example.order.Order;
import org.example.orderbook.DirectOrder;

public interface MatchingCallback {

    void performMatch(OrderCommand cmd, Order incoming, DirectOrder restingOrder, long tradeSize);

}
