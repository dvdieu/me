package org.example.common.command;

import org.example.common.MatcherTradeEvent;
import org.example.order.Order;
import org.example.order.OrderSide;
import org.example.order.OrderType;
import org.example.order.TimeInForce;

import java.util.ArrayList;
import java.util.List;

public class OrderCommand extends Order {

    public OrderCommandType command;
    public boolean stopAfterFirstCommand;

    public MatcherTradeEvent matcherEvent;
    public MatcherTradeEvent matcherEventTail;


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

    public void addTradeEvent(MatcherTradeEvent event) {
        if(matcherEvent == null) {
            matcherEvent = event;
        } else {
            matcherEventTail.nextEvent = event;
        }
        matcherEventTail = event;
    }

    public List<MatcherTradeEvent> extractEvents() {
        List<MatcherTradeEvent> extractedTradeEvents = new ArrayList<>();

        MatcherTradeEvent mte = this.matcherEvent;
        while (mte != null) {
            extractedTradeEvents.add(mte);
            mte = mte.nextEvent;
        }

        return extractedTradeEvents;
    }
}
