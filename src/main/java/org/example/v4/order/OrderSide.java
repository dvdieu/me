package org.example.v4.order;

public enum OrderSide {
    BUY,
    SELL

    ;

    public OrderSide getOpposite() {
        return this == BUY ? SELL : BUY;
    }
}
