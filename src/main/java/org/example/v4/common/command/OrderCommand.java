package org.example.v4.common.command;

import org.example.v4.common.MatcherTradeEvent;
import org.example.v4.order.Order;
import org.example.v4.order.OrderSide;
import org.example.v4.order.OrderType;
import org.example.v4.order.TimeInForce;

import java.util.ArrayList;
import java.util.List;

public class OrderCommand {

    public OrderCommandType command;

    public long orderId;
    public long userId;
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
    public boolean postOnly;
    public boolean stopAfterFirstCommand;

    public List<MatcherTradeEvent> matcherEvents = new ArrayList<>();


    public static OrderCommand cancel(long orderId, long uid) {
        OrderCommand cmd = new OrderCommand();
        cmd.command = OrderCommandType.CANCEL_ORDER;
        cmd.orderId = orderId;
        cmd.userId = uid;
        return cmd;
    }


    private static OrderCommand place(long id, long userId, OrderSide side, OrderType type, TimeInForce tif, long price, long quantity) {
        OrderCommand cmd = new OrderCommand();
        cmd.command = OrderCommandType.PLACE_ORDER;

        cmd.orderId = id;
        cmd.userId = userId;
        cmd.side = side;
        cmd.type = type;
        cmd.timeInForce = tif;
        cmd.price = price;
        cmd.totalQuantity = quantity;
        cmd.remainingQuantity = quantity;
        return cmd;
    }

    public static OrderCommand createStandardOrder(long id, long userId, OrderSide side, OrderType type, TimeInForce tif, long price, long quantity) {
        OrderCommand order = place(id, userId, side, type, tif, price, quantity);
        order.stopPrice = 0;
        order.displayedQuantity = quantity;
        order.hiddenQuantity = 0;
        order.isIceberg = false;
        order.icebergPeak = 0;

        return order;
    }


    public static OrderCommand createStopOrder(long id, long userId, OrderSide side, OrderType type, TimeInForce tif, long stopPrice, long price, long quantity) {
        OrderCommand order = place(id, userId, side, type, tif, price, quantity);
        order.stopPrice = stopPrice;
        order.displayedQuantity = quantity;
        order.hiddenQuantity = 0;
        order.isIceberg = false;
        order.icebergPeak = 0;

        return order;
    }

    public static OrderCommand createIcebergOrder(long id, long userId, OrderSide side, long price, long quantity, long icebergPeak) {
        OrderCommand order = place(id, userId, side, OrderType.LIMIT, TimeInForce.GTC, price, quantity);
        order.stopPrice = 0;
        order.displayedQuantity = Math.min(quantity, icebergPeak);
        order.hiddenQuantity = quantity - order.displayedQuantity;
        order.isIceberg = true;
        order.icebergPeak = icebergPeak;

        return order;
    }

    public static OrderCommand createStopIcebergOrder(long id, long userId, OrderSide side, OrderType type, long stopPrice, long price, long quantity, long icebergPeak) {
        OrderCommand order = createIcebergOrder(id, userId, side, price, quantity, icebergPeak);
        order.stopPrice = stopPrice;
        order.type = type;

        return order;
    }


    public OrderCommand postOnly() {
        this.postOnly = true;
        return this;
    }

    public Order buildOrder() {
        Order order = new Order();
        order.id = orderId;
        order.userId = userId;
        order.side = side;
        order.type = type;
        order.timeInForce = timeInForce;
        order.price = price;
        order.stopPrice = stopPrice;
        order.totalQuantity = totalQuantity;
        order.remainingQuantity = remainingQuantity;
        order.displayedQuantity = displayedQuantity;
        order.hiddenQuantity = hiddenQuantity;
        order.isIceberg = isIceberg;
        order.icebergPeak = icebergPeak;

        return order;
    }

}
