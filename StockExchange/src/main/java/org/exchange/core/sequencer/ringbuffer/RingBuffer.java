package org.exchange.core.sequencer.ringbuffer;

import org.exchange.core.util.Order;

public interface RingBuffer {
    void addOrder(long orderId, long qty, long price, boolean isBuy);
    Order readOrder(int consumerId);
}
