package org.example.order;

public class Order {

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

        details += " {id:" + orderId + ", user:" + userId + ", TIF:" + timeInForce + "}";
        return details;
    }

    public boolean isPriceAcceptable(long offerPrice) {
        if(price == 0) return true;
        return (side == OrderSide.BUY) ? (offerPrice <= price) : (offerPrice >= price);
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

    public void correctOverfilledIcebergDisplay() {
        if(this.displayedQuantity < 0) {
            if(this.remainingQuantity > 0) {
                this.displayedQuantity = Math.min(this.icebergPeak, this.remainingQuantity);
                this.hiddenQuantity = this.remainingQuantity - this.displayedQuantity;
            } else {
                this.displayedQuantity = 0;
                this.hiddenQuantity = 0;
            }
        }
    }

}
