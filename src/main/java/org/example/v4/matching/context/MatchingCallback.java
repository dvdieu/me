package org.example.v4.matching.context;

import org.example.v4.order.Order;

import java.util.Iterator;

public interface MatchingCallback {

    void performMatch(MatchingContext context, Iterator<Order> restingIterator, Order restingOrder, long tradeSize);

}
