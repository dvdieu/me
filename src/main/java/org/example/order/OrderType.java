package org.example.order;

public enum OrderType {
    LIMIT,
    MARKET,
    STOP_LIMIT(LIMIT),
    STOP_MARKET(MARKET),

    ;

    public final OrderType executableType;

    OrderType() {
        this.executableType = this;
    }

    OrderType(OrderType executableType) {
        this.executableType = executableType;
    }
}
