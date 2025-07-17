package org.example.orderbook;

import org.example.order.Order;

public class DirectOrder extends Order {

    public PriceLevel priceLevel;

    public DirectOrder prev;
    public DirectOrder next;

    public DirectOrder(Order order, PriceLevel priceLevel) {
        this.orderId = order.orderId;
        this.userId = order.userId;
        this.side = order.side;
        this.type = order.type;
        this.timeInForce = order.timeInForce;
        this.price = order.price;
        this.stopPrice = order.stopPrice;
        this.totalQuantity = order.totalQuantity;
        this.remainingQuantity = order.remainingQuantity;
        this.displayedQuantity = order.displayedQuantity;
        this.hiddenQuantity = order.hiddenQuantity;
        this.isIceberg = order.isIceberg;
        this.icebergPeak = order.icebergPeak;
        this.postOnly = order.postOnly;

        this.priceLevel = priceLevel;
    }

    public void remove() {
        priceLevel.removeOrderVolume(this);

        if(prev != null) {
            prev.next = next;
        }

        if(next != null) {
            next.prev = prev;
        }

        if(this == priceLevel.head) {
            priceLevel.head = prev;
        }

        if(this == priceLevel.tail) {
            priceLevel.tail = next;
        }
    }
}
