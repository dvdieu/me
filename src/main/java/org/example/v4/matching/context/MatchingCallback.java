package org.example.v4.matching.context;

import org.example.v4.order.Order;
import org.example.v4.orderbook.DirectOrder;

public interface MatchingCallback {

    void performMatch(Order incoming, DirectOrder restingOrder, long tradeSize);

}
