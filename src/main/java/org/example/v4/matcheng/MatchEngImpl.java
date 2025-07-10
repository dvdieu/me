package org.example.v4.matcheng;

import org.example.v4.common.L2MarketData;
import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.common.command.CommandResultCode;
import org.example.v4.common.command.OrderCommand;
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

public class MatchEngImpl implements MatchEng {

    private final MatchingHandler matchingHandler;
    private final OrderBook orderBook = new OrderBook();
    private final StopBook stopBook = new StopBook();
    private final Deque<Order> commandQueue = new LinkedList<>();
    private final Map<Long, DirectOrder> orders = new HashMap<>();
    private final MatchingContext matchingContext = new MatchingContext(this::performMatch);

    public MatchEngImpl() {
        this(MatchingStrategy.FIFO);
    }

    public MatchEngImpl(MatchingStrategy matchingStrategy) {
        matchingHandler = MatchingHandlerFactory.getMatchingProcessor(matchingStrategy);
    }


    private long lastTradePrice = 0;


    @Override
    public CommandResultCode placeOrder(OrderCommand cmd) {
        if(orders.containsKey(cmd.orderId)) {
            cmd.addTradeEvent(MatcherTradeEvent.createRejectEvent(cmd, cmd.remainingQuantity));
            System.out.println("duplicate orderId: " + cmd.orderId);
            return CommandResultCode.SUCCESS;
        }

        if(cmd.type == OrderType.STOP_MARKET || cmd.type == OrderType.STOP_LIMIT) {
            if(cmd.stopPrice != lastTradePrice) {
                DirectOrder directOrder = stopBook.addStopOrder(cmd);
                orders.put(cmd.orderId, directOrder);
                System.out.println("-> Stop order added (waiting for trigger)");
                logOrderBookState();
                return CommandResultCode.SUCCESS;
            }

            cmd.type = cmd.type.executableType;
        }

        commandQueue.add(cmd);

        while(!commandQueue.isEmpty()) {
            Order incoming = commandQueue.poll();
            System.out.println("\nProcess order: " + cmd);
            processIncomingOrder(cmd, incoming);

            if(!commandQueue.isEmpty() && cmd.stopAfterFirstCommand) {
                System.out.println("\n\n Incoming order queue");
                for (Order o : commandQueue) {
                    System.out.println(o);
                }

                break;
            }
        }

        logOrderBookState();
        return CommandResultCode.SUCCESS;
    }

    @Override
    public CommandResultCode cancelOrder(OrderCommand cmd) {
        DirectOrder order = orders.remove(cmd.orderId);
        if(order == null || cmd.userId != order.userId) {
            return CommandResultCode.MATCHING_UNKNOWN_ORDER_ID;
        }

        System.out.println("--------------------------------------------------------");
        System.out.println("Cancel order: " + order);

        if(order.type == OrderType.LIMIT || order.type == OrderType.MARKET) {
            orderBook.removeOrder(order);
            cmd.addTradeEvent(MatcherTradeEvent.createReduceEvent(order, order.remainingQuantity, true));
        } else {
            stopBook.removeOrder(order);
        }

        logOrderBookState();
        return CommandResultCode.SUCCESS;
    }

    @Override
    public Order getOrderById(long orderId) {
        return orders.get(orderId);
    }


    private void processIncomingOrder(OrderCommand cmd, Order incoming) {
        if(incoming.timeInForce == TimeInForce.FOK) {
            long needed = incoming.remainingQuantity;
            long available = calculatePotentialFill(incoming);
            if(available < needed) {
                System.out.println("-> FOK check FAILED: needed " + needed + ", available " + available);
                cmd.addTradeEvent(MatcherTradeEvent.createRejectEvent(incoming, incoming.remainingQuantity));
                return;
            }
        }

        if(incoming.postOnly) {
            PriceLevel priceLevel = orderBook.getBestLevel(incoming.side.getOpposite());
            if(priceLevel != null && incoming.isPriceAcceptable(priceLevel.price)) {
                System.out.println("-> Post Only check FAILED: limitPrice: " + incoming.price + ", bestPrice: " + priceLevel.price);
                cmd.addTradeEvent(MatcherTradeEvent.createRejectEvent(incoming, incoming.remainingQuantity));
                return;
            }
        }

        tryMatchInstantly(cmd, incoming);
        incoming.correctOverfilledIcebergDisplay();

        if(incoming.remainingQuantity > 0) {
            if(incoming.timeInForce == TimeInForce.GTC) {
                DirectOrder directOrder = orderBook.addOrder(incoming);
                orders.put(incoming.orderId, directOrder);
                System.out.println("-> Partially filled, " + incoming.remainingQuantity + " remaining added to book as resting order");
            } else {
                cmd.addTradeEvent(MatcherTradeEvent.createRejectEvent(incoming, incoming.remainingQuantity));
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

            available += entry.getValue().getRemainingQuantityWithoutUser(order.userId);

            if(prevPrice != 0) {
                available += stopBook.calculateLiquidity(order, prevPrice, price);
            }

            prevPrice = price;
        }

        return available;
    }


    private void tryMatchInstantly(OrderCommand cmd, Order incoming) {
        matchingContext.initContext(cmd, incoming);

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
                orders.put(refilledOrder.orderId, directOrder);
                System.out.printf("-> Iceberg order %d refilled [displayed=%d, hidden=%d] and placed at end of queue\n", refilledOrder.orderId, refilledOrder.displayedQuantity, refilledOrder.remainingQuantity);
            }

            triggerStopOrders(cmd, incoming, prevTradePrice, lastTradePrice);

            if(priceLevel.isEmpty()) {
                orderBook.removeLevel(sideOpposite, priceLevel.price);
            }
            priceLevel = orderBook.getBestLevel(sideOpposite);
        }

        matchingContext.selfMatchOrders.forEach(order -> {
            DirectOrder directOrder = orderBook.addOrder(order);
            orders.put(order.orderId, directOrder);
        });
    }

    private void triggerStopOrders(OrderCommand cmd, Order incoming, long prevPrice, long lastPrice) {
        List<DirectOrder> triggeredStopOrders = stopBook.getTriggeredStopOrders(prevPrice, lastPrice);
        for (DirectOrder stopOrder : triggeredStopOrders) {
            System.out.printf("-> Triggered: %s %s (id=%d) at trigger price %d" + (stopOrder.type == OrderType.STOP_LIMIT? ", limitPrice = " + stopOrder.price : "") + "\n", stopOrder.side, stopOrder.type, stopOrder.orderId, stopOrder.stopPrice);

            stopOrder.convertToExecutableOrderType();

            if(incoming.side == stopOrder.side) {
                boolean shouldAddToCommandQueue = stopOrder.type == OrderType.MARKET ||
                        (incoming.side == OrderSide.BUY && stopOrder.price >= lastPrice) ||
                        (incoming.side == OrderSide.SELL && stopOrder.price <= lastPrice);

                if (shouldAddToCommandQueue) {
                    orders.remove(stopOrder.orderId);
                    commandQueue.add(stopOrder);
                    System.out.println("Add to command queue");
                } else {
                    DirectOrder directOrder = orderBook.addOrder(stopOrder);
                    orders.put(stopOrder.orderId, directOrder);
                    System.out.println("Add to order book");
                }
            }
            else {
                boolean shouldMatch = stopOrder.type == OrderType.MARKET ||
                                (stopOrder.side == OrderSide.SELL && stopOrder.price <= lastPrice) ||
                                (stopOrder.side == OrderSide.BUY && stopOrder.price >= lastPrice);

                if (shouldMatch) {
                    orders.remove(stopOrder.orderId);
                    matchDirectOrderInStopBook(cmd, incoming, stopOrder);
                } else {
                    DirectOrder directOrder = orderBook.addOrder(stopOrder);
                    orders.put(stopOrder.orderId, directOrder);
                    System.out.println("Add to order book");
                }
            }
        }
    }

    private void matchDirectOrderInStopBook(OrderCommand cmd, Order incoming, Order stopOrder) {
        if(incoming.remainingQuantity == 0 || incoming.isSelfMatch(stopOrder) || stopOrder.postOnly ||
                (stopOrder.timeInForce == TimeInForce.FOK && incoming.remainingQuantity < stopOrder.remainingQuantity)) {
            commandQueue.add(stopOrder);
            System.out.println("Add to command queue");
            return;
        }

        long tradeSize = Math.min(incoming.remainingQuantity, stopOrder.remainingQuantity);
        System.out.printf("Trade: %s (Maker) %d vs %s (Taker) %d @%d => %d\n",
                stopOrder.side, stopOrder.orderId, incoming.side, incoming.orderId, lastTradePrice, tradeSize);

        incoming.matching(stopOrder, tradeSize);
        cmd.addTradeEvent(MatcherTradeEvent.createTradeEvent(incoming, stopOrder, lastTradePrice, tradeSize));

        stopOrder.correctOverfilledIcebergDisplay();
        if(stopOrder.remainingQuantity > 0) {
            commandQueue.add(stopOrder);
        }
    }

    private void performMatch(OrderCommand cmd, Order incoming, DirectOrder resting, long tradeSize) {
        lastTradePrice = resting.price;
        incoming.matching(resting, tradeSize);
        System.out.printf("Trade: %s (Maker) %d vs %s (Taker) %d @%d => %d\n",
                resting.side, resting.orderId, incoming.side, incoming.orderId, resting.price, tradeSize);

        resting.priceLevel.removeTradeVolume(resting.userId, tradeSize);
        cmd.addTradeEvent(MatcherTradeEvent.createTradeEvent(incoming, resting, resting.price, tradeSize));

        if(resting.displayedQuantity == 0) {
            resting.remove();
            orders.remove(resting.orderId);
        }
    }


    private void logOrderBookState() {
        System.out.println();
        orderBook.logOrderBookState();
        stopBook.logStopBookState();
        System.out.println();
    }

    @Override
    public L2MarketData getL2MarketData() {
        return orderBook.getL2MarketDataSnapshot();
    }


    @Override
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

            int expectedBucketOrders = 0;
            long expectedBucketRemainingQuantity = 0;
            long expectedBucketDisplayedQuantity = 0;

            if(order == null) {
                thrw("order is null");
            }
            if (order.next != null) {
                thrw("best order has not-null next reference");
            }

            while (order != null) {
                expectedBucketOrders++;
                expectedBucketRemainingQuantity += order.remainingQuantity;
                expectedBucketDisplayedQuantity += order.displayedQuantity;

                if (ordersInChain.containsKey(order.orderId)) {
                    thrw("duplicate orderid in the chain");
                }
                ordersInChain.put(order.orderId, order);

                if (lastOrder != null && order.next != lastOrder) {
                    thrw("incorrect next reference");
                }
                if (priceLevel.price != order.price) {
                    thrw("price differs");
                }

                if(order.priceLevel != priceLevel) {
                    thrw("unexpected price level");
                }

                final PriceLevel knownBucket = bucketsFoundInChain.get(order.price);
                if (knownBucket == null) {
                    bucketsFoundInChain.put(order.price, order.priceLevel);
                } else if (knownBucket != order.priceLevel) {
                    thrw("found two different buckets having same price");
                }

                if (side != order.side) {
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

            if (priceLevel.tail != lastOrder) {
                thrw("last order is not a tail");
            }

            if (priceLevel.remainingQuantity != expectedBucketRemainingQuantity) {
                thrw("bucket remaining quantity does not match orders chain sizes");
            }
            if (priceLevel.displayedQuantity != expectedBucketDisplayedQuantity) {
                thrw("bucket remaining quantity does not match orders chain sizes");
            }
            if (priceLevel.numOrders != expectedBucketOrders) {
                thrw("bucket numOrders does not match orders chain length");
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

            int expectedBucketOrders = 0;
            long expectedBucketRemainingQuantity = 0;
            long expectedBucketDisplayedQuantity = 0;

            if(order == null) {
                thrw("order is null");
            }
            if (order.next != null) {
                thrw("best order has not-null next reference");
            }

            while (order != null) {
                expectedBucketOrders++;
                expectedBucketRemainingQuantity += order.remainingQuantity;
                expectedBucketDisplayedQuantity += order.displayedQuantity;

                if (ordersInChain.containsKey(order.orderId)) {
                    thrw("duplicate orderid in the chain");
                }
                ordersInChain.put(order.orderId, order);

                if (lastOrder != null && order.next != lastOrder) {
                    thrw("incorrect next reference");
                }
                if (priceLevel.price != order.stopPrice) {
                    thrw("price differs");
                }

                if(order.priceLevel != priceLevel) {
                    thrw("unexpected price level");
                }

                final PriceLevel knownBucket = bucketsFoundInChain.get(order.stopPrice);
                if (knownBucket == null) {
                    bucketsFoundInChain.put(order.stopPrice, order.priceLevel);
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

            if (priceLevel.tail != lastOrder) {
                thrw("last order is not a tail");
            }

            if (priceLevel.remainingQuantity != expectedBucketRemainingQuantity) {
                thrw("bucket remaining quantity does not match orders chain sizes");
            }
            if (priceLevel.displayedQuantity != expectedBucketDisplayedQuantity) {
                thrw("bucket remaining quantity does not match orders chain sizes");
            }
            if (priceLevel.numOrders != expectedBucketOrders) {
                thrw("bucket numOrders does not match orders chain length");
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
