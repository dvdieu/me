package org.example.order;

public enum OrderSide {
    BUY,
    SELL

    ;

    public OrderSide getOpposite() {
        return this == BUY ? SELL : BUY;
    }
}
