package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.matching.MatchingHandler;
import org.example.v4.matching.MatchingHandlerFactory;
import org.example.v4.matching.MatchingStrategy;
import org.example.v4.matching.context.MatchingContext;
import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;
import org.example.v4.order.OrderType;
import org.example.v4.order.TimeInForce;
import org.example.v4.orderbook.DirectOrder;
import org.example.v4.orderbook.OrderBook;
import org.example.v4.orderbook.PriceLevel;
import org.example.v4.orderbook.StopBook;

import java.util.*;

public class MatchEng {

    private final MatchingHandler matchingHandler;
    private final OrderBook orderBook = new OrderBook();
    private final StopBook stopBook = new StopBook();
    private final Deque<Order> commandQueue = new LinkedList<>();
    private final Map<Long, DirectOrder> orders = new HashMap<>();
    private final MatchingContext matchingContext = new MatchingContext(this::performMatch);

    public MatchEng() {
        this(MatchingStrategy.FIFO);
    }

    public MatchEng(MatchingStrategy matchingStrategy) {
        matchingHandler = MatchingHandlerFactory.getMatchingProcessor(matchingStrategy);
    }


    private long lastTradePrice = 0;


    public void placeOrder(Order order) {
        System.out.println("--------------------------------------------------------");
        System.out.println("Placing order: " + order);

        if(orders.containsKey(order.id)) {
            order.matcherTradeEvents.add(MatcherTradeEvent.createRejectEvent(order.remainingQuantity));
            System.out.println("duplicate order id: " + order.id);
            return;
        }

        if(order.type == OrderType.STOP_MARKET || order.type == OrderType.STOP_LIMIT) {
            if(order.stopPrice != lastTradePrice) {
                DirectOrder directOrder = stopBook.addStopOrder(order);
                orders.put(order.id, directOrder);
                System.out.println("-> Stop order added (waiting for trigger)");
                logOrderBookState();
                return;
            }

            order.convertToExecutableOrderType();
        }

        commandQueue.add(order);

        while(!commandQueue.isEmpty()) {
            Order incoming = commandQueue.poll();
            System.out.println("\nProcess order: " + order);
            processIncomingOrder(incoming);

            if(!commandQueue.isEmpty() && order.stopAfterFirstCommand) {
                System.out.println("\n\n Incoming order queue");
                for (Order o : commandQueue) {
                    System.out.println(o);
                }

                break;
            }

        }

        logOrderBookState();
    }

    public Order cancelOrder(long orderId) {
        DirectOrder directOrder = orders.remove(orderId);
        if(directOrder == null) {
            return null;
        }

        Order order = directOrder.order;
        System.out.println("--------------------------------------------------------");
        System.out.println("Cancel order: " + order);

        if(order.type == OrderType.LIMIT || order.type == OrderType.MARKET) {
            orderBook.removeOrder(directOrder);
            order.matcherTradeEvents.add(MatcherTradeEvent.createReduceEvent(order.remainingQuantity));
        } else {
            stopBook.removeOrder(directOrder);
        }

        logOrderBookState();
        return order;
    }

    public Order getOrderById(long orderId) {
        DirectOrder directOrder = orders.get(orderId);
        return directOrder == null ? null : directOrder.order;
    }

    private void processIncomingOrder(Order incoming) {
        if(incoming.timeInForce == TimeInForce.FOK) {
            long needed = incoming.remainingQuantity;
            long available = calculatePotentialFill(incoming);
            if(available < needed) {
                System.out.println("-> FOK check FAILED: needed " + needed + ", available " + available);
                incoming.matcherTradeEvents.addFirst(MatcherTradeEvent.createRejectEvent(incoming.remainingQuantity));
                return;
            }
        }

        if(incoming.postOnly) {
            PriceLevel priceLevel = orderBook.getBestLevel(incoming.side.getOpposite());
            if(priceLevel != null && incoming.isPriceAcceptable(priceLevel.price)) {
                System.out.println("-> Post Only check FAILED: limitPrice: " + incoming.price + ", bestPrice: " + priceLevel.price);
                incoming.matcherTradeEvents.addFirst(MatcherTradeEvent.createRejectEvent(incoming.remainingQuantity));
                return;
            }
        }

        tryMatchInstantly(incoming);
        incoming.correctOverfilledIcebergDisplay();

        if(incoming.remainingQuantity > 0) {
            if(incoming.timeInForce == TimeInForce.GTC) {
                DirectOrder directOrder = orderBook.addOrder(incoming);
                orders.put(incoming.id, directOrder);
                System.out.println("-> Partially filled, " + incoming.remainingQuantity + " remaining added to book as resting order");
            } else {
                incoming.matcherTradeEvents.addFirst(MatcherTradeEvent.createRejectEvent(incoming.remainingQuantity));
                System.out.println("-> IOC leftover cancelled: " + incoming.remainingQuantity + " not filled");
            }
        }
    }


    private long calculatePotentialFill(Order order) {
        long available = 0;
        long needed = order.remainingQuantity;

        OrderSide oppositeSide = order.side.getOpposite();
        Iterator<Map.Entry<Long, PriceLevel>> iterator = orderBook.getLevels(oppositeSide).entrySet().iterator();

        long prevPrice = lastTradePrice;
        while(iterator.hasNext() && available < needed) {
            Map.Entry<Long, PriceLevel> entry = iterator.next();
            long price = entry.getKey();

            if(!order.isPriceAcceptable(price)) {
                break;
            }

            available += entry.getValue().orderStream()
                    .filter(e -> !order.isSelfMatch(e.order))
                    .mapToLong(e -> e.order.remainingQuantity).sum();

            available += stopBook.calculateLiquidity(order, prevPrice, price);

            prevPrice = price;
        }

        return available;
    }


    private void tryMatchInstantly(Order incoming) {
        matchingContext.initContext(incoming);

        OrderSide sideOpposite = incoming.side.getOpposite();
        PriceLevel priceLevel = orderBook.getBestLevel(sideOpposite);
        while (incoming.remainingQuantity > 0 && priceLevel != null) {
            long bestPrice = priceLevel.price;
            if(!incoming.isPriceAcceptable(bestPrice)) {
                break;
            }

            long prevTradePrice = lastTradePrice == 0? bestPrice : lastTradePrice;

            matchingContext.updatePriceLevel(priceLevel);
            matchingHandler.tryMatchInstantly(matchingContext);

            for (Order refilledOrder : matchingContext.refilledOrders) {
                DirectOrder directOrder = orderBook.addOrder(refilledOrder);
                orders.put(refilledOrder.id, directOrder);
                System.out.printf("-> Iceberg order %d refilled [displayed=%d, hidden=%d] and placed at end of queue\n", refilledOrder.id, refilledOrder.displayedQuantity, refilledOrder.remainingQuantity);
            }

            triggerStopOrders(incoming, prevTradePrice, lastTradePrice);

            if(priceLevel.isEmpty()) {
                orderBook.removeLevel(sideOpposite, priceLevel.price);
            }
            priceLevel = orderBook.getBestLevel(sideOpposite);
        }

        matchingContext.selfMatchOrders.forEach(order -> {
            DirectOrder directOrder = orderBook.addOrder(order);
            orders.put(order.id, directOrder);
        });
    }

    private void triggerStopOrders(Order incoming, long prevPrice, long lastPrice) {
        List<Order> triggeredStopOrders = stopBook.getTriggeredStopOrders(prevPrice, lastPrice);
        for (Order stopOrder : triggeredStopOrders) {
            System.out.printf("-> Triggered: %s %s (id=%d) at trigger price %d" + (stopOrder.type == OrderType.STOP_LIMIT? ", limitPrice = " + stopOrder.price : "") + "\n", stopOrder.side, stopOrder.type, stopOrder.id, stopOrder.stopPrice);

            stopOrder.convertToExecutableOrderType();

            if(incoming.side == stopOrder.side) {
                boolean shouldAddToCommandQueue = stopOrder.type == OrderType.MARKET ||
                        (incoming.side == OrderSide.BUY && stopOrder.price >= lastPrice) ||
                        (incoming.side == OrderSide.SELL && stopOrder.price <= lastPrice);

                if (shouldAddToCommandQueue) {
                    orders.remove(stopOrder.id);
                    commandQueue.add(stopOrder);
                    System.out.println("Add to command queue");
                } else {
                    DirectOrder directOrder = orderBook.addOrder(stopOrder);
                    orders.put(stopOrder.id, directOrder);
                    System.out.println("Add to order book");
                }
            }
            else {
                boolean shouldMatch = stopOrder.type == OrderType.MARKET ||
                                (stopOrder.side == OrderSide.SELL && stopOrder.price <= lastPrice) ||
                                (stopOrder.side == OrderSide.BUY && stopOrder.price >= lastPrice);

                if (shouldMatch) {
                    orders.remove(stopOrder.id);
                    matchDirectOrderInStopBook(incoming, stopOrder);
                } else {
                    DirectOrder directOrder = orderBook.addOrder(stopOrder);
                    orders.put(stopOrder.id, directOrder);
                    System.out.println("Add to order book");
                }
            }
        }
    }

    private void matchDirectOrderInStopBook(Order incoming, Order stopOrder) {
        if(incoming.remainingQuantity == 0 || incoming.isSelfMatch(stopOrder) ||
                (stopOrder.timeInForce == TimeInForce.FOK && incoming.remainingQuantity < stopOrder.remainingQuantity)) {
            commandQueue.add(stopOrder);
            System.out.println("Add to command queue");
            return;
        }

        long tradeSize = Math.min(incoming.remainingQuantity, stopOrder.remainingQuantity);
        System.out.printf("Trade: %s (Maker) %d vs %s (Taker) %d @%d => %d\n",
                stopOrder.side, stopOrder.id, incoming.side, incoming.id, lastTradePrice, tradeSize);

        incoming.matching(stopOrder, tradeSize);
        incoming.matcherTradeEvents.add(MatcherTradeEvent.createTradeEvent(stopOrder.id, lastTradePrice, tradeSize));

        stopOrder.correctOverfilledIcebergDisplay();
        if(stopOrder.remainingQuantity > 0) {
            commandQueue.add(stopOrder);
        }
    }

    private void performMatch(Order incoming, DirectOrder restingDirect, long tradeSize) {
        Order resting = restingDirect.order;
        lastTradePrice = resting.price;
        incoming.matching(resting, tradeSize);
        System.out.printf("Trade: %s (Maker) %d vs %s (Taker) %d @%d => %d\n",
                resting.side, resting.id, incoming.side, incoming.id, resting.price, tradeSize);

        incoming.matcherTradeEvents.add(MatcherTradeEvent.createTradeEvent(resting.id, resting.price, tradeSize));

        if(resting.displayedQuantity == 0) {
            restingDirect.remove();
            orders.remove(resting.id);
        }
    }


    private void logOrderBookState() {
        System.out.println();
        orderBook.logOrderBookState();
        stopBook.logStopBookState();
        System.out.println();
    }

    public L2MarketData getL2MarketData() {
        return orderBook.getL2MarketDataSnapshot();
    }


    public void validateInternalState() {
        final TreeMap<Long, DirectOrder> ordersInChain = new TreeMap<>();
        validateChain(OrderSide.SELL, ordersInChain);
        validateChain(OrderSide.BUY, ordersInChain);
        validateStopBook(ordersInChain);

        orders.forEach((k, v) -> {
            if (ordersInChain.remove(k) != v) {
                thrw("chained orders does not contain orderId=" + k);
            }
        });

        if (!ordersInChain.isEmpty()) {
            thrw("orderIdIndex does not contain each order from chains");
        }
    }

    private void validateChain(OrderSide side, TreeMap<Long, DirectOrder> ordersInChain) {
        TreeMap<Long, PriceLevel> buckets = orderBook.getLevels(side);
        final TreeMap<Long, PriceLevel> bucketsFoundInChain = new TreeMap<>();

        long lastPrice = -1;
        for (PriceLevel priceLevel : buckets.values()) {
            DirectOrder lastOrder = null;
            DirectOrder order = priceLevel.head;

            if(order == null) {
                thrw("order is null");
            }
            if (order.next != null) {
                thrw("best order has not-null next reference");
            }

            while (order != null) {
                if (ordersInChain.containsKey(order.order.id)) {
                    thrw("duplicate orderid in the chain");
                }
                ordersInChain.put(order.order.id, order);

                if (lastOrder != null && order.next != lastOrder) {
                    thrw("incorrect next reference");
                }
                if (order.priceLevel.price != order.order.price) {
                    thrw("price differs");
                }

                if(order.priceLevel != priceLevel) {
                    thrw("unexpected price level");
                }

                final PriceLevel knownBucket = bucketsFoundInChain.get(order.order.price);
                if (knownBucket == null) {
                    bucketsFoundInChain.put(order.order.price, order.priceLevel);
                } else if (knownBucket != order.priceLevel) {
                    thrw("found two different buckets having same price");
                }

                if (side != order.order.side) {
                    thrw("not expected order action");
                }

                lastOrder = order;
                order = order.prev;
            }

            if (lastPrice != -1 && ((side == OrderSide.BUY && priceLevel.price >= lastPrice) ||
                    (side == OrderSide.SELL && priceLevel.price <= lastPrice))) {
                thrw("unexpected price change direction");
            }
            lastPrice = priceLevel.price;

            if (lastOrder.priceLevel.tail != lastOrder) {
                thrw("last order is not a tail");
            }
        }

        buckets.forEach((price, bucket) -> {
            if (bucketsFoundInChain.remove(price) != bucket) thrw("bucket in the price-tree not found in the chain");
        });

        if (!bucketsFoundInChain.isEmpty()) {
            thrw("found buckets in the chain that not discoverable from the price-tree");
        }
    }
    private void validateStopBook(TreeMap<Long, DirectOrder> ordersInChain) {
        TreeMap<Long, PriceLevel> buckets = stopBook.getStopLevels();
        final TreeMap<Long, PriceLevel> bucketsFoundInChain = new TreeMap<>();

        long lastPrice = -1;
        for (PriceLevel priceLevel : buckets.values()) {
            DirectOrder lastOrder = null;
            DirectOrder order = priceLevel.head;

            if(order == null) {
                thrw("order is null");
            }
            if (order.next != null) {
                thrw("best order has not-null next reference");
            }

            while (order != null) {
                if (ordersInChain.containsKey(order.order.id)) {
                    thrw("duplicate orderid in the chain");
                }
                ordersInChain.put(order.order.id, order);

                if (lastOrder != null && order.next != lastOrder) {
                    thrw("incorrect next reference");
                }
                if (order.priceLevel.price != order.order.stopPrice) {
                    thrw("price differs");
                }

                if(order.priceLevel != priceLevel) {
                    thrw("unexpected price level");
                }

                final PriceLevel knownBucket = bucketsFoundInChain.get(order.order.stopPrice);
                if (knownBucket == null) {
                    bucketsFoundInChain.put(order.order.stopPrice, order.priceLevel);
                } else if (knownBucket != order.priceLevel) {
                    thrw("found two different buckets having same price");
                }

                lastOrder = order;
                order = order.prev;
            }

            if (lastPrice != -1 && priceLevel.price <= lastPrice) {
                thrw("unexpected price change direction");
            }
            lastPrice = priceLevel.price;

            if (lastOrder.priceLevel.tail != lastOrder) {
                thrw("last order is not a tail");
            }
        }

        buckets.forEach((price, bucket) -> {
            if (bucketsFoundInChain.remove(price) != bucket) thrw("bucket in the price-tree not found in the chain");
        });

        if (!bucketsFoundInChain.isEmpty()) {
            thrw("found buckets in the chain that not discoverable from the price-tree");
        }
    }

    private void thrw(final String msg) {
        throw new IllegalStateException(msg);
    }
}
