package org.example.v4.order;

import org.example.v4.common.MatcherTradeEvent;

import java.util.ArrayList;
import java.util.List;

public class Order {

    public long id;
    public int userId;
    public OrderSide side;
    public OrderType type;
    public TimeInForce timeInForce;
    public long price;
    public long stopPrice;
    public long totalQuantity;
    public long remainingQuantity;
    public long displayedQuantity;
    public long hiddenQuantity;
    public boolean isIceberg;
    public long icebergPeak;
    public List<MatcherTradeEvent> matcherTradeEvents = new ArrayList<>();


    private Order(long id, int userId, OrderSide side, OrderType type, TimeInForce tif, long price, long quantity) {
        this.id = id;
        this.userId = userId;
        this.side = side;
        this.type = type;
        this.timeInForce = tif;
        this.price = price;
        this.totalQuantity = quantity;
        this.remainingQuantity = quantity;
    }

    public static Order createStandardOrder(long id, int userId, OrderSide side, OrderType type, TimeInForce tif, long price, long quantity) {
        Order order = new Order(id, userId, side, type, tif, price, quantity);
        order.stopPrice = 0;
        order.displayedQuantity = quantity;
        order.hiddenQuantity = 0;
        order.isIceberg = false;
        order.icebergPeak = 0;

        return order;
    }


    public static Order createStopOrder(long id, int userId, OrderSide side, OrderType type, TimeInForce tif, long stopPrice, long price, long quantity) {
        Order order = new Order(id, userId, side, type, tif, price, quantity);
        order.stopPrice = stopPrice;
        order.displayedQuantity = quantity;
        order.hiddenQuantity = 0;
        order.isIceberg = false;
        order.icebergPeak = 0;

        return order;
    }

    public static Order createIcebergOrder(long id, int userId, OrderSide side, long price, long quantity, long icebergPeak) {
        Order order = new Order(id, userId, side, OrderType.LIMIT, TimeInForce.GTC, price, quantity);
        order.stopPrice = 0;
        order.displayedQuantity = Math.min(quantity, icebergPeak);
        order.hiddenQuantity = quantity - order.displayedQuantity;
        order.isIceberg = true;
        order.icebergPeak = icebergPeak;

        return order;
    }

    public static Order createStopIcebergOrder(long id, int userId, OrderSide side, OrderType type, long stopPrice, long price, long quantity, long icebergPeak) {
        Order order = createIcebergOrder(id, userId, side, price, quantity, icebergPeak);
        order.stopPrice = stopPrice;
        order.type = type;

        return order;
    }

    public Order createIcebergChild() {
        if (this.displayedQuantity > 0) return this;
        if (!this.isIceberg || this.hiddenQuantity <= 0) return null;

        long newDisplay = Math.min(this.hiddenQuantity, this.icebergPeak);
        this.displayedQuantity = newDisplay;
        this.hiddenQuantity -= newDisplay;

        return this;
    }

    public String toString() {
        String details = side + " " + type + (isIceberg ? "-ICEBERG" : "") + " " + remainingQuantity;
        if (type == OrderType.LIMIT || type == OrderType.STOP_LIMIT) {
            details += " @" + (price == 0 ? "MKT" : price);
        }

        if (type == OrderType.STOP_MARKET || type == OrderType.STOP_LIMIT) {
            details += " (stop=" + stopPrice + ")";
        }

        if (isIceberg) {
            details += " [displayed=" + displayedQuantity + ", hidden=" + hiddenQuantity + "]";
        }

        details += " {id:" + id + ", user:" + userId + ", TIF:" + timeInForce + "}";
        return details;
    }

    public boolean isPriceUnacceptable(long offerPrice) {
        if(price == 0) return false;
        return (side == OrderSide.BUY) ? (offerPrice > price) : (offerPrice < price);
    }

    public boolean isSelfMatch(Order resting) {
        return userId == resting.userId;
    }

    public void matching(Order makerOrder, long tradeSize) {
        this.remainingQuantity -= tradeSize;
        this.displayedQuantity -= tradeSize;

        makerOrder.remainingQuantity -= tradeSize;
        makerOrder.displayedQuantity -= tradeSize;
    }

    public void convertToExecutableOrderType() {
        if(type == OrderType.STOP_LIMIT) {
            type = OrderType.LIMIT;
        } else {
            type = OrderType.MARKET;
        }
    }

    public void correctOverfilledIcebergDisplay() {
        if(this.displayedQuantity < 0) {
            if(this.remainingQuantity > 0) {
                this.displayedQuantity = Math.min(this.icebergPeak, this.remainingQuantity);
                this.hiddenQuantity = this.remainingQuantity - this.displayedQuantity;
            } else {
                this.displayedQuantity = 0;
            }
        }
    }
}
