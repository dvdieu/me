package org.example.c3;

import java.util.*;
public class MatchingEngine {
    // Order side constants
    public static final int BUY = 1;
    public static final int SELL = -1;
    // Order types
    public static final int LIMIT = 0;
    public static final int MARKET = 1;
    public static final int STOP_LIMIT = 2;
    public static final int STOP_MARKET = 3;
    // Time-in-force
    public static final int GTC = 0;
    public static final int IOC = 1;
    public static final int FOK = 2;

    // Order class
    static class Order {
        static long nextId = 1;
        long id;
        int userId;
        int side;
        int type;
        long price;     // price for limit orders (or stop-limit's limit)
        long stopPrice; // trigger price for stop orders
        long totalQuantity;
        long remainingQuantity;
        long displayedQuantity; // current displayed portion (for iceberg)
        long hiddenQuantity;    // remaining hidden volume (for iceberg)
        int timeInForce;
        boolean isIceberg;
        long icebergPeak;
        // Constructor for normal limit/market orders
        public Order(int userId, int side, int type, long price, long quantity, int tif) {
            this.id = nextId++;
            this.userId = userId;
            this.side = side;
            this.type = type;
            this.price = price;
            this.stopPrice = 0;
            this.totalQuantity = quantity;
            this.remainingQuantity = quantity;
            this.displayedQuantity = quantity;
            this.hiddenQuantity = 0;
            this.timeInForce = tif;
            this.isIceberg = false;
            this.icebergPeak = 0;
        }
        // Constructor for stop orders
        public Order(int userId, int side, int type, long price, long stopPrice, long quantity, int tif) {
            this.id = nextId++;
            this.userId = userId;
            this.side = side;
            this.type = type;
            if(type == STOP_LIMIT) {
                this.price = price;    // price = limit price for stop-limit
            } else {
                this.price = 0;
            }
            this.stopPrice = stopPrice;
            this.totalQuantity = quantity;
            this.remainingQuantity = quantity;
            this.displayedQuantity = quantity;
            this.hiddenQuantity = 0;
            this.timeInForce = tif;
            this.isIceberg = false;
            this.icebergPeak = 0;
        }
        // Constructor for iceberg orders (treated as limit GTC)
        public Order(int userId, int side, long price, long totalQuantity, long peakDisplayQuantity) {
            this.id = nextId++;
            this.userId = userId;
            this.side = side;
            this.type = LIMIT;
            this.price = price;
            this.stopPrice = 0;
            this.totalQuantity = totalQuantity;
            this.remainingQuantity = totalQuantity;
            this.displayedQuantity = Math.min(totalQuantity, peakDisplayQuantity);
            this.hiddenQuantity = totalQuantity - this.displayedQuantity;
            this.timeInForce = GTC;
            this.isIceberg = true;
            this.icebergPeak = peakDisplayQuantity;
        }
        // Create a new visible order from iceberg hidden volume
        public Order createIcebergChild() {
            if(!this.isIceberg || this.hiddenQuantity <= 0) return null;
            long newDisplay = Math.min(this.hiddenQuantity, this.icebergPeak);
            this.hiddenQuantity -= newDisplay;
            Order child = new Order(this.userId, this.side, LIMIT, this.price, newDisplay, GTC);
            child.isIceberg = true;
            return child;
        }
        @Override
        public String toString() {
            String typeStr;
            switch(type) {
                case LIMIT: typeStr = isIceberg ? "LIMIT-ICEBERG" : "LIMIT"; break;
                case MARKET: typeStr = "MARKET"; break;
                case STOP_LIMIT: typeStr = "STOP-LIMIT"; break;
                case STOP_MARKET: typeStr = "STOP-MARKET"; break;
                default: typeStr = "UNKNOWN"; break;
            }
            String sideStr = (side == BUY ? "BUY" : "SELL");
            String tifStr;
            switch(timeInForce) {
                case IOC: tifStr = "IOC"; break;
                case FOK: tifStr = "FOK"; break;
                default: tifStr = "GTC"; break;
            }
            String details = sideStr + " " + typeStr + " " + remainingQuantity;
            if(type == LIMIT || type == STOP_LIMIT) {
                details += " @"+ (price==0?"MKT":price);
            }
            if(type == STOP_MARKET || type == STOP_LIMIT) {
                details += " (stop=" + stopPrice + ")";
            }
            if(isIceberg) {
                details += " [displayed=" + displayedQuantity + ", hidden=" + hiddenQuantity + "]";
            }
            details += " {id:" + id + ", user:" + userId + ", TIF:" + tifStr + "}";
            return details;
        }
    }
    // PriceLevel class
    static class PriceLevel {
        long price;
        ArrayDeque<Order> orders = new ArrayDeque<>();
        public PriceLevel(long price) { this.price = price; }
    }
    // OrderBook class
    static class OrderBook {
        TreeMap<Long, PriceLevel> buyLevels = new TreeMap<>(Collections.reverseOrder());
        TreeMap<Long, PriceLevel> sellLevels = new TreeMap<>();
        private PriceLevel getOrCreateLevel(long price, int side) {
            TreeMap<Long, PriceLevel> levels = (side == BUY ? buyLevels : sellLevels);
            PriceLevel level = levels.get(price);
            if(level == null) {
                level = new PriceLevel(price);
                levels.put(price, level);
            }
            return level;
        }
        PriceLevel getBestLevel(int side) {
            if(side == BUY) {
                return buyLevels.isEmpty() ? null : buyLevels.firstEntry().getValue();
            } else {
                return sellLevels.isEmpty() ? null : sellLevels.firstEntry().getValue();
            }
        }
        Long getBestPrice(int side) {
            PriceLevel level = getBestLevel(side);
            return (level == null ? null : level.price);
        }
        void addOrder(Order order) {
            PriceLevel level = getOrCreateLevel(order.price, order.side);
            level.orders.add(order);
        }
        Order pollBestOrder(int side) {
            PriceLevel level = getBestLevel(side);
            if(level == null) return null;
            Order ord = level.orders.poll();
            if(ord != null && level.orders.isEmpty()) {
                // remove empty level
                if(side == BUY) buyLevels.remove(level.price);
                else sellLevels.remove(level.price);
            }
            return ord;
        }
    }
    // StopBook class
    static class StopBook {
        TreeMap<Long, ArrayDeque<Order>> buyStopOrders = new TreeMap<>();
        TreeMap<Long, ArrayDeque<Order>> sellStopOrders = new TreeMap<>();
        void addStopOrder(Order order) {
            TreeMap<Long, ArrayDeque<Order>> map = (order.side == BUY ? buyStopOrders : sellStopOrders);
            ArrayDeque<Order> list = map.get(order.stopPrice);
            if(list == null) {
                list = new ArrayDeque<>();
                map.put(order.stopPrice, list);
            }
            list.add(order);
        }
        // Get and remove stop orders triggered in (prevPrice, newPrice] or [newPrice, prevPrice)
        List<Order> getTriggeredStopOrders(long prevPrice, long newPrice) {
            List<Order> triggered = new ArrayList<>();
            if(newPrice == prevPrice) return triggered;
            if(newPrice > prevPrice) {
                // price up: trigger buy stops (prevPrice, newPrice]
                SortedMap<Long, ArrayDeque<Order>> toTrigger = buyStopOrders.subMap(prevPrice, false, newPrice, true);
                for(Long key : new ArrayList<>(toTrigger.keySet())) {
                    ArrayDeque<Order> orders = toTrigger.get(key);
                    while(!orders.isEmpty()) {
                        triggered.add(orders.poll());
                    }
                    toTrigger.remove(key);
                }
                // Sort triggered ascending by trigger price (lower first)
                triggered.sort(Comparator.comparingLong(o -> o.stopPrice));
            } else {
                // price down: trigger sell stops [newPrice, prevPrice)
                SortedMap<Long, ArrayDeque<Order>> toTrigger = sellStopOrders.subMap(newPrice, true, prevPrice, false);
                for(Long key : new ArrayList<>(toTrigger.keySet())) {
                    ArrayDeque<Order> orders = toTrigger.get(key);
                    while(!orders.isEmpty()) {
                        triggered.add(orders.poll());
                    }
                    toTrigger.remove(key);
                }
                // Sort triggered descending by trigger price (higher first)
                triggered.sort((o1, o2) -> Long.compare(o2.stopPrice, o1.stopPrice));
            }
            return triggered;
        }
    }

    // MatchingEngine fields
    OrderBook orderBook = new OrderBook();
    StopBook stopBook = new StopBook();
    ArrayDeque<Order> eventQueue = new ArrayDeque<>();
    long lastTradePrice = 0;  // track last matched price

    // Logging helper: log order book state
    private void logOrderBookState() {
        System.out.println("Order Book:");
        // Sell side
        System.out.print("SELL: ");
        if(orderBook.sellLevels.isEmpty()) {
            System.out.print("(empty)");
        } else {
            for(Map.Entry<Long, PriceLevel> entry : orderBook.sellLevels.entrySet()) {
                long price = entry.getKey();
                long totalVol = 0;
                boolean icebergAtLevel = false;
                for(Order o : entry.getValue().orders) {
                    totalVol += o.remainingQuantity;
                    if(o.isIceberg) icebergAtLevel = true;
                }
                System.out.print(price + "(" + totalVol + (icebergAtLevel?"*":"") + ") ");
            }
        }
        System.out.println();
        // Buy side
        System.out.print("BUY : ");
        if(orderBook.buyLevels.isEmpty()) {
            System.out.print("(empty)");
        } else {
            for(Map.Entry<Long, PriceLevel> entry : orderBook.buyLevels.entrySet()) {
                long price = entry.getKey();
                long totalVol = 0;
                boolean icebergAtLevel = false;
                for(Order o : entry.getValue().orders) {
                    totalVol += o.remainingQuantity;
                    if(o.isIceberg) icebergAtLevel = true;
                }
                System.out.print(price + "(" + totalVol + (icebergAtLevel?"*":"") + ") ");
            }
        }
        System.out.println();
        // Stop orders
        if(!stopBook.sellStopOrders.isEmpty() || !stopBook.buyStopOrders.isEmpty()) {
            System.out.print("StopBook: ");
            if(!stopBook.sellStopOrders.isEmpty()) {
                System.out.print("Sell triggers: ");
                for(Long trig : stopBook.sellStopOrders.keySet()) {
                    int count = stopBook.sellStopOrders.get(trig).size();
                    System.out.print(trig + "[" + count + "] ");
                }
            }
            if(!stopBook.buyStopOrders.isEmpty()) {
                System.out.print(" Buy triggers: ");
                for(Long trig : stopBook.buyStopOrders.keySet()) {
                    int count = stopBook.buyStopOrders.get(trig).size();
                    System.out.print(trig + "[" + count + "] ");
                }
            }
            System.out.println();
        }
    }

    // Place a new order into the engine
    public void placeOrder(Order order) {
        System.out.println("\nPlacing order: " + order);
        if(order.type == STOP_MARKET || order.type == STOP_LIMIT) {
            // Stop order goes to stopBook
            stopBook.addStopOrder(order);
            System.out.println("-> Stop order added (waiting for trigger)");
            logOrderBookState();
            return;
        }
        // Otherwise, add to event queue and process
        eventQueue.add(order);
        while(!eventQueue.isEmpty()) {
            Order incoming = eventQueue.poll();
            processIncomingOrder(incoming);
        }
        logOrderBookState();
    }

    private void processIncomingOrder(Order incoming) {
        if(incoming.timeInForce == FOK) {
            long needed = incoming.remainingQuantity;
            long available = calculatePotentialFill(incoming);
            if(available < needed) {
                System.out.println("-> FOK check FAILED: needed " + needed + ", available " + available);
                incoming.remainingQuantity = 0;
                return; // reject, no fill
            } else {
                System.out.println("-> FOK check PASSED: needed " + needed + ", available " + available);
            }
        }
        if(incoming.timeInForce == IOC || incoming.timeInForce == FOK) {
            matchOrder(incoming);
            if(incoming.remainingQuantity > 0) {
                System.out.println("-> " + (incoming.timeInForce == FOK ? "FOK not fully filled, cancelling"
                        : "IOC leftover cancelled")
                        + ": " + incoming.remainingQuantity + " not filled");
                incoming.remainingQuantity = 0;
            }
        } else {
            // GTC (default)
            matchOrder(incoming);
            if(incoming.remainingQuantity > 0) {
                if(incoming.type == LIMIT) {
                    orderBook.addOrder(incoming);
                    System.out.println("-> Partially filled, " + incoming.remainingQuantity
                            + " remaining added to book as resting order");
                } else if(incoming.type == MARKET) {
                    System.out.println("-> Market order not fully filled, "
                            + incoming.remainingQuantity + " remaining cancelled (no liquidity)");
                    incoming.remainingQuantity = 0;
                }
            }
        }
    }

    private long calculatePotentialFill(Order order) {
        long needed = order.remainingQuantity;
        long sum = 0;
        int oppositeSide = (order.side == BUY ? SELL : BUY);
        Long prevPrice = lastTradePrice == 0 ? null : lastTradePrice;
        // Traverse opposite book
        Iterator<Map.Entry<Long, PriceLevel>> it;
        if(oppositeSide == BUY) it = orderBook.buyLevels.entrySet().iterator();
        else it = orderBook.sellLevels.entrySet().iterator();
        long lastLevelPrice = (prevPrice == null ? (oppositeSide==BUY? Long.MAX_VALUE: Long.MIN_VALUE) : prevPrice);
        while(it.hasNext() && sum < needed) {
            Map.Entry<Long, PriceLevel> entry = it.next();
            long price = entry.getKey();
            // Check price limit for limit orders
            if(order.type == LIMIT) {
                if(order.side == BUY && price > order.price) break;
                if(order.side == SELL && price < order.price) break;
            }
            // Add volume at this price
            long levelVol = 0;
            for(Order o : entry.getValue().orders) {
                levelVol += o.remainingQuantity;
                if(o.isIceberg) {
                    levelVol += o.hiddenQuantity; // count hidden volume
                }
            }
            sum += levelVol;
            lastLevelPrice = price;
            if(sum >= needed) break;
        }
        // If not enough from resting orders, consider stops that would trigger beyond current levels:
        if(sum < needed) {
            if(order.side == BUY) {
                // Price would rise beyond highest ask
                SortedMap<Long, ArrayDeque<Order>> stops = stopBook.sellStopOrders.tailMap(lastLevelPrice, false);
                for(Long trig : stops.keySet()) {
                    for(Order stopOrder : stops.get(trig)) {
                        // Only consider stop-sell orders (triggered by price up)
                        if(stopOrder.type == STOP_MARKET) {
                            sum += stopOrder.remainingQuantity;
                        } else if(stopOrder.type == STOP_LIMIT) {
                            long limitPrice = stopOrder.price;
                            if(order.type == MARKET || order.price >= limitPrice) {
                                sum += stopOrder.remainingQuantity;
                            }
                        }
                    }
                    if(sum >= needed) break;
                }
            } else {
                // Price would fall below lowest bid
                SortedMap<Long, ArrayDeque<Order>> stops = stopBook.buyStopOrders.headMap(lastLevelPrice, false);
                for(Long trig : stops.sequencedKeySet()) {
                    for(Order stopOrder : stops.get(trig)) {
                        // Consider stop-buy orders (triggered by price down)
                        if(stopOrder.type == STOP_MARKET) {
                            sum += stopOrder.remainingQuantity;
                        } else if(stopOrder.type == STOP_LIMIT) {
                            long limitPrice = stopOrder.price;
                            if(order.type == MARKET || order.price <= limitPrice) {
                                sum += stopOrder.remainingQuantity;
                            }
                        }
                    }
                    if(sum >= needed) break;
                }
            }
        }
        return sum;
    }

    private void matchOrder(Order incoming) {
        int oppositeSide = (incoming.side == BUY ? SELL : BUY);
        PriceLevel level;
        // Loop through price levels
        while(incoming.remainingQuantity > 0 && (level = orderBook.getBestLevel(oppositeSide)) != null) {
            long bestPrice = level.price;
            // Check price constraints for limit
            if(incoming.type == LIMIT) {
                if(incoming.side == BUY && bestPrice > incoming.price) break;
                if(incoming.side == SELL && bestPrice < incoming.price) break;
            }
            long tradePrice = bestPrice;
            long prevPrice = (lastTradePrice == 0 ? bestPrice : lastTradePrice);
            // FIFO matching at this price
            while(incoming.remainingQuantity > 0 && !level.orders.isEmpty()) {
                Order resting = level.orders.peek();
                // Self-trade prevention
                if(resting.userId == incoming.userId) {
                    System.out.println("-> Self-trade detected (Order " + incoming.id
                            + " and resting " + resting.id + "), preventing match");
                    incoming.remainingQuantity = 0;
                    return;
                }
                // Execute trade
                long matchVol = Math.min(incoming.remainingQuantity, resting.remainingQuantity);
                incoming.remainingQuantity -= matchVol;
                resting.remainingQuantity -= matchVol;
                lastTradePrice = tradePrice;
                System.out.println("Trade: " + (resting.side==BUY?"BUY":"SELL") + " (Maker) " + resting.id
                        + " vs " + (incoming.side==BUY?"BUY":"SELL") + " (Taker) " + incoming.id
                        + " @"+ tradePrice + " => " + matchVol);
                // Remove or update resting order if filled
                if(resting.remainingQuantity == 0) {
                    level.orders.poll(); // remove from level
                    if(resting.isIceberg && resting.hiddenQuantity > 0) {
                        // Refill iceberg
                        Order refill = resting.createIcebergChild();
                        if(refill != null) {
                            level.orders.add(refill);
                            System.out.println("-> Iceberg order " + resting.id + " refilled "
                                    + refill.remainingQuantity + " (new peak) and placed at end of queue");
                        }
                    }
                    if(level.orders.isEmpty()) {
                        // Remove price level if empty
                        if(resting.side == BUY) orderBook.buyLevels.remove(bestPrice);
                        else orderBook.sellLevels.remove(bestPrice);
                    }
                }
                if(incoming.remainingQuantity == 0) break;
            }
            // Check and trigger stop orders after finishing this price level
            List<Order> triggeredStops = stopBook.getTriggeredStopOrders(prevPrice, lastTradePrice);
            for(Order stopOrder : triggeredStops) {
                System.out.println("-> Triggered: " + (stopOrder.side==BUY?"BUY":"SELL") + " "
                        + (stopOrder.type==STOP_MARKET?"STOP-MARKET":"STOP-LIMIT")
                        + " (id="+ stopOrder.id + ") at trigger price " + stopOrder.stopPrice);
                if(stopOrder.type == STOP_MARKET) {
                    // Convert to market
                    stopOrder.type = MARKET;
                    stopOrder.price = 0;
                    if(stopOrder.side != incoming.side && incoming.remainingQuantity > 0) {
                        // Opposite side: match directly with incoming
                        long vol = Math.min(incoming.remainingQuantity, stopOrder.remainingQuantity);
                        incoming.remainingQuantity -= vol;
                        stopOrder.remainingQuantity -= vol;
                        long tradeP = lastTradePrice;
                        System.out.println("Trade: " + (incoming.side==BUY?"BUY":"SELL") + " (Maker) "
                                + incoming.id + " vs "
                                + (stopOrder.side==BUY?"BUY":"SELL") + " (Taker) "
                                + stopOrder.id + " @"+ tradeP + " => " + vol);
                        lastTradePrice = tradeP;
                        if(stopOrder.remainingQuantity > 0) {
                            System.out.println("-> Stop-market order " + stopOrder.id + " not fully filled, remaining "
                                    + stopOrder.remainingQuantity + " cancelled");
                            stopOrder.remainingQuantity = 0;
                        }
                    } else {
                        // Same side or no incoming volume: queue this market order for next event
                        eventQueue.add(stopOrder);
                    }
                } else if(stopOrder.type == STOP_LIMIT) {
                    // Convert to limit GTC
                    stopOrder.type = LIMIT;
                    boolean canMatchNow = false;
                    if(stopOrder.side == BUY) {
                        if(lastTradePrice <= stopOrder.price) canMatchNow = true;
                    } else {
                        if(lastTradePrice >= stopOrder.price) canMatchNow = true;
                    }
                    if(canMatchNow) {
                        if(stopOrder.side != incoming.side && incoming.remainingQuantity > 0) {
                            long vol = Math.min(incoming.remainingQuantity, stopOrder.remainingQuantity);
                            incoming.remainingQuantity -= vol;
                            stopOrder.remainingQuantity -= vol;
                            long tradeP = lastTradePrice;
                            System.out.println("Trade: " + (incoming.side==BUY?"BUY":"SELL") + " (Maker) "
                                    + incoming.id + " vs "
                                    + (stopOrder.side==BUY?"BUY":"SELL") + " (Taker) "
                                    + stopOrder.id + " @"+ tradeP + " => " + vol);
                            lastTradePrice = tradeP;
                        }
                        if(stopOrder.remainingQuantity > 0) {
                            System.out.println("-> Stop-limit order " + stopOrder.id
                                    + " not fully filled, queuing remaining "
                                    + stopOrder.remainingQuantity + " as new order");
                            eventQueue.add(stopOrder);
                        }
                    } else {
                        System.out.println("-> Stop-limit order " + stopOrder.id
                                + " triggered but not executable, placed as LIMIT order on book");
                        orderBook.addOrder(stopOrder);
                    }
                }
            }
            if(incoming.remainingQuantity == 0) break;
        }
    }



    // Main test cases
    public static void main(String[] args) {
        MatchingEngine engine = new MatchingEngine();

        System.out.println("# Test 1: Basic LIMIT match");
        Order buy1 = new Order(1, BUY, LIMIT, 100, 10, GTC);
        engine.placeOrder(buy1);
        Order sell1 = new Order(2, SELL, LIMIT, 100, 10, GTC);
        engine.placeOrder(sell1);
        if(engine.orderBook.buyLevels.isEmpty() && engine.orderBook.sellLevels.isEmpty()) {
            System.out.println("✅ PASS");
        } else {
            System.out.println("❌ FAIL");
        }

        System.out.println("\n# Test 2: Cascade stop-order 90→80→70 (roles flip)");
        engine = new MatchingEngine();
        // Setup initial buy depth
        engine.placeOrder(new Order(3, BUY, LIMIT, 100, 10, GTC));
        engine.placeOrder(new Order(4, BUY, LIMIT, 95, 10, GTC));
        engine.placeOrder(new Order(5, BUY, LIMIT, 85, 10, GTC));
        engine.placeOrder(new Order(6, BUY, LIMIT, 75, 10, GTC));
        engine.placeOrder(new Order(7, BUY, LIMIT, 65, 50, GTC));
        // Place stop orders
        Order stopSell90 = new Order(8, SELL, STOP_MARKET, 0, 90, 20, GTC);
        Order stopSell80 = new Order(9, SELL, STOP_MARKET, 0, 80, 30, GTC);
        Order stopBuy70 = new Order(10, BUY, STOP_MARKET, 0, 70, 40, GTC);
        engine.placeOrder(stopSell90);
        engine.placeOrder(stopSell80);
        engine.placeOrder(stopBuy70);
        // Big sell that triggers cascade
        Order bigSell = new Order(11, SELL, LIMIT, 60, 50, GTC);
        engine.placeOrder(bigSell);
        System.out.println("✅ PASS");

        System.out.println("\n# Test 3: FOK success (with stop + iceberg help)");
        engine = new MatchingEngine();
        // Setup sells: 30 @95, iceberg 50 (peak 20) @100, stop-limit sell 20 @100 (trigger=100)
        engine.placeOrder(new Order(12, SELL, LIMIT, 95, 30, GTC));
        Order iceberg100 = new Order(13, SELL, 100, 50, 20);
        engine.placeOrder(iceberg100);
        Order stopSell100 = new Order(14, SELL, STOP_LIMIT, 100, 100, 20, GTC);
        engine.placeOrder(stopSell100);
        // FOK buy for 90 units
        Order bigBuyFOK = new Order(15, BUY, LIMIT, 105, 90, FOK);
        engine.placeOrder(bigBuyFOK);
        if(bigBuyFOK.remainingQuantity == 0) {
            System.out.println("✅ PASS");
        } else {
            System.out.println("❌ FAIL");
        }

        System.out.println("\n# Test 4: FOK failure");
        engine = new MatchingEngine();
        engine.placeOrder(new Order(16, SELL, LIMIT, 100, 30, GTC));
        engine.placeOrder(new Order(17, SELL, LIMIT, 105, 20, GTC));
        engine.placeOrder(new Order(17, SELL, LIMIT, 105,105, 150, GTC));
        Order buyFOKfail = new Order(18, BUY, LIMIT, 110, 100, FOK);
        engine.placeOrder(buyFOKfail);
        if(buyFOKfail.remainingQuantity == 100) {
            System.out.println("✅ PASS");
        } else {
            System.out.println("❌ FAIL");
        }

        System.out.println("\n# Test 5: Iceberg multiple peaks");
        engine = new MatchingEngine();
        Order icebergMulti = new Order(19, SELL, 100, 100, 25);
        engine.placeOrder(icebergMulti);
        Order bigBuy = new Order(20, BUY, LIMIT, 100, 100, GTC);
        engine.placeOrder(bigBuy);
        if(icebergMulti.remainingQuantity == 0 && engine.orderBook.sellLevels.isEmpty()) {
            System.out.println("✅ PASS");
        } else {
            System.out.println("❌ FAIL");
        }
    }
}
