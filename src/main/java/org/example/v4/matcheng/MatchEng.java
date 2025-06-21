package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.matching.MatchingHandler;
import org.example.v4.matching.MatchingHandlerFactory;
import org.example.v4.matching.MatchingStrategy;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;
import org.example.v4.order.OrderType;
import org.example.v4.order.TimeInForce;
import org.example.v4.orderbook.OrderBook;
import org.example.v4.orderbook.PriceLevel;
import org.example.v4.orderbook.StopBook;

import java.util.*;

public class MatchEng {

    private final MatchingHandler matchingHandler;
    private final OrderBook orderBook = new OrderBook();
    private final StopBook stopBook = new StopBook();
    private final Deque<Order> commandQueue = new LinkedList<>();
    private final MatchingContext matchingContext = new MatchingContext();

    public MatchEng() {
        this(MatchingStrategy.FIFO);
    }

    public MatchEng(MatchingStrategy matchingStrategy) {
        matchingHandler = MatchingHandlerFactory.getMatchingProcessor(matchingStrategy);
    }


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
        matchingContext.initContext(incoming);

        OrderSide sideOpposite = incoming.side.getOpposite();
        PriceLevel priceLevel = orderBook.getBestLevel(sideOpposite);
        while (incoming.remainingQuantity > 0 && priceLevel != null) {
            long bestPrice = priceLevel.price;
            if(incoming.isPriceUnacceptable(bestPrice)) {
                break;
            }

            long prevTradePrice = lastTradePrice == 0? bestPrice : lastTradePrice;

            matchingContext.updatePriceLevel(priceLevel);
            matchingHandler.tryMatchInstantly(matchingContext);
            matchingContext.refilledOrders.forEach(orderBook::addOrder);
            if(matchingContext.lastTradePrice != 0) {
                lastTradePrice = matchingContext.lastTradePrice;
            }

            triggerStopOrders(incoming, prevTradePrice, lastTradePrice);

            if(priceLevel.isEmpty()) {
                orderBook.removeLevel(sideOpposite, priceLevel.price);
            }
            priceLevel = orderBook.getBestLevel(sideOpposite);
        }

        matchingContext.selfMatchOrders.forEach(orderBook::addOrder);
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
