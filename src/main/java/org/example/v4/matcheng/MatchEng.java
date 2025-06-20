package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;
import org.example.v4.order.OrderType;
import org.example.v4.order.TimeInForce;
import org.example.v4.orderbook.OrderBook;
import org.example.v4.orderbook.PriceLevel;
import org.example.v4.orderbook.StopBook;

import java.util.*;

public class MatchEng {

    private final OrderBook orderBook = new OrderBook();
    private final StopBook stopBook = new StopBook();
    private final Deque<Order> commandQueue = new LinkedList<>();

    private long lastTradePrice = 0;


    public void placeOrder(Order order) {
        System.out.println("Placing order: " + order);

        if(order.type == OrderType.STOP_MARKET || order.type == OrderType.STOP_LIMIT) {
            // TODO: check if can trigger now

            stopBook.addStopOrder(order);
            System.out.println("-> Stop order added (waiting for trigger)");
            logOrderBookState();
            return;
        }

        commandQueue.add(order);

        while(!commandQueue.isEmpty()) {
            Order incoming = commandQueue.poll();
            processIncomingOrder(incoming);
        }

        System.out.println();
        logOrderBookState();
    }

    private void processIncomingOrder(Order incoming) {
        if(incoming.timeInForce == TimeInForce.FOK) {
            long needed = incoming.remainingQuantity;
            long available = calculatePotentialFill(incoming);
            if(available < needed) {
                System.out.println("-> FOK check FAILED: needed " + needed + ", available " + available);
                return;
            }
        }

        matchOrder(incoming);

        if(incoming.remainingQuantity > 0) {
            if(incoming.timeInForce == TimeInForce.GTC) {
                orderBook.addOrder(incoming);
                System.out.println("-> Partially filled, " + incoming.remainingQuantity + " remaining added to book as resting order");
            } else {
                System.out.println("-> IOC leftover cancelled: " + incoming.remainingQuantity + " not filled");
            }
        }
    }


    private long calculatePotentialFill(Order order) {
        long available = 0;
        long needed = order.remainingQuantity;

        OrderSide oppositeSide = order.side.getOpposite();
        Iterator<Map.Entry<Long, PriceLevel>> iterator = orderBook.getLevels(oppositeSide).entrySet().iterator();

        while(iterator.hasNext() && available < needed) {
            Map.Entry<Long, PriceLevel> entry = iterator.next();
            long price = entry.getKey();

            if(order.isPriceUnacceptable(price)) {
                break;
            }

            available += entry.getValue().orders.stream()
                    .filter(e -> !order.isSelfMatch(e))
                    .mapToLong(e -> e.remainingQuantity).sum();
        }

        return available;
    }


    private void matchOrder(Order incoming) {
        Deque<Order> selfMatchOrders = new LinkedList<>();
        Deque<Order> refilledOrders = new LinkedList<>();

        PriceLevel priceLevel = orderBook.getBestLevel(incoming.side.getOpposite());
        while (incoming.remainingQuantity > 0 && priceLevel != null) {
            long bestPrice = priceLevel.price;
            if(incoming.isPriceUnacceptable(bestPrice)) {
                break;
            }

            long prevTradePrice = lastTradePrice == 0? bestPrice : lastTradePrice;

            tryMatchAtPriceLevel(incoming, priceLevel, selfMatchOrders, refilledOrders);

            triggerStopOrders(incoming, prevTradePrice, lastTradePrice);


            refilledOrders.forEach(orderBook::addOrder);
            refilledOrders.clear();

            priceLevel = orderBook.getBestLevel(incoming.side.getOpposite());
        }

        selfMatchOrders.forEach(orderBook::addOrder);
    }

    private void tryMatchAtPriceLevel(Order incoming, PriceLevel priceLevel, Deque<Order> selfMatchOrders, Deque<Order> refilledOrders) {
        Iterator<Order> iterator = priceLevel.orders.iterator();
        while (incoming.remainingQuantity > 0 && iterator.hasNext()) {
            Order resting = iterator.next();
            if(incoming.isSelfMatch(resting)) {
                iterator.remove();
                selfMatchOrders.add(resting);
                continue;
            }

            lastTradePrice = priceLevel.price;
            long tradeSize = Math.min(incoming.remainingQuantity, resting.displayedQuantity);
            incoming.matching(resting, tradeSize);
            System.out.printf("Trade: %s (Maker) %d vs %s (Taker) %d @%d => %d\n",
                    resting.side, resting.id, incoming.side, incoming.id, priceLevel.price, tradeSize);

            if(resting.displayedQuantity == 0) {
                iterator.remove();
                if(priceLevel.isEmpty()) {
                    orderBook.removeLevel(resting.side, priceLevel.price);
                }

                Order icebergChild = resting.createIcebergChild();
                if(icebergChild != null) {
                    refilledOrders.add(icebergChild);
                }
            }
        }
    }

    private void triggerStopOrders(Order incoming, long prevPrice, long lastPrice) {
        List<Order> triggeredStopOrders = stopBook.getTriggeredStopOrders(prevPrice, lastPrice);
        for (Order order : triggeredStopOrders) {

        }

    }


    private void logOrderBookState() {
        orderBook.logOrderBookState();
        stopBook.logStopBookState();
    }

    public L2MarketData getL2MarketData() {
        return orderBook.getL2MarketDataSnapshot();
    }
}
