package org.example.v4.orderbook;


import java.util.Spliterator;
import java.util.function.Consumer;

public final class OrdersSpliterator implements Spliterator<DirectOrder> {

    private DirectOrder pointer;

    public OrdersSpliterator(DirectOrder pointer) {
        this.pointer = pointer;
    }

    @Override
    public boolean tryAdvance(Consumer<? super DirectOrder> action) {
        if (pointer == null) {
            return false;
        } else {
            action.accept(pointer);
            pointer = pointer.prev;
            return true;
        }
    }

    @Override
    public Spliterator<DirectOrder> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return Spliterator.ORDERED;
    }
}
