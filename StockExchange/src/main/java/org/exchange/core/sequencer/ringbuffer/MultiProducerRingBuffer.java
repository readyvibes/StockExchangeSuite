package org.exchange.core.sequencer.ringbuffer;

import org.exchange.core.util.Order;

import java.util.concurrent.atomic.AtomicLong;

public class MultiProducerRingBuffer implements RingBuffer{
    private final int ringBufferLength;
    private final Order[] ringBuffer;

    private final Sequence producerOffset = new Sequence(0);
    private final Sequence[] consumerOffsets;
    private final Sequence minConsumerOffset = new Sequence(0);

    private final Sequence[] availableBuffer;

    private final AtomicLong idGenerator = new AtomicLong(1);

    private final int totalConsumers;

    public MultiProducerRingBuffer(int totalConsumers) {
        this.ringBufferLength = 2097152;
        this.ringBuffer = new Order[ringBufferLength];
        this.totalConsumers = totalConsumers;
        this.consumerOffsets = new Sequence[this.totalConsumers];
        this.availableBuffer = new Sequence[this.ringBufferLength];
        for (int i = 0; i < ringBufferLength; i++) {
            ringBuffer[i] = new Order(-1, 0, -1, false);
        }

        for (int i = 0; i < totalConsumers; i++) {
            consumerOffsets[i] = new Sequence(0);
        }

        for (int i = 0; i < ringBufferLength; i++) {
            availableBuffer[i] = new Sequence(-1);
        }
    }

    public MultiProducerRingBuffer(int totalConsumers, int ringBufferLength) throws Exception{
        if (ringBufferLength < 1 || (ringBufferLength & (ringBufferLength - 1)) != 0) {
            throw new Exception("Ring Buffer Length should be a power of 2");
        }
        this.ringBufferLength = ringBufferLength;
        this.ringBuffer = new Order[ringBufferLength];
        this.totalConsumers = totalConsumers;
        this.consumerOffsets = new Sequence[this.totalConsumers];
        this.availableBuffer = new Sequence[this.ringBufferLength];

        for (int i = 0; i < ringBufferLength; i++) {
            ringBuffer[i] = new Order(-1, 0, -1, false);
        }

        for (int i = 0; i < totalConsumers; i++) {
            consumerOffsets[i] = new Sequence(0);
        }

        for (int i = 0; i < ringBufferLength; i++) {
            availableBuffer[i] = new Sequence(-1);
        }
    }


    public void addOrder(long qty, long price, boolean isBuy) {
        long currentOffset = producerOffset.getAndIncrement();
        long generatedId = idGenerator.getAndIncrement();
        int index = (int) (currentOffset & (ringBufferLength - 1));

        long currMinConsumerOffset = minConsumerOffset.get();
        if (currentOffset - currMinConsumerOffset >= ringBufferLength) {
            currMinConsumerOffset = getMinimumConsumerOffset();
            while (currentOffset - currMinConsumerOffset >= ringBufferLength) {
                Thread.onSpinWait();
                currMinConsumerOffset = getMinimumConsumerOffset();
            }
        }

        ringBuffer[index].update(generatedId, qty, price, isBuy);
        availableBuffer[index].set(currentOffset);
    }

    public Order readOrder(int consumerId) {
        Sequence consumerOffset = consumerOffsets[consumerId];
        long currentOffset = consumerOffset.get();
        int index = (int) (currentOffset & (ringBufferLength - 1));

        while (availableBuffer[index].get() != currentOffset) {
            Thread.onSpinWait();
        }

        Order order = ringBuffer[index];

        long nextOffset = currentOffset + 1;
        consumerOffset.lazySet(nextOffset);

        if ((nextOffset & 0x7F) == 0) {
            updateMinConsumerOffset(nextOffset);
        }

        return order;
    }

    private void updateMinConsumerOffset(long current) {
        if (consumerOffsets.length == 1) {
            minConsumerOffset.lazySet(current);
        } else {
            long min = current;
            for (Sequence s : consumerOffsets) {
                min = Math.min(min, s.get());
            }
            minConsumerOffset.lazySet(min);
        }
    }

    private long getMinimumConsumerOffset() {
        return minConsumerOffset.get();
    }

    public long getProducerOffset() {
        return producerOffset.get();
    }

    private static class Sequence extends AtomicLong {
        public long p1, p2, p3, p4, p5, p6, p7;
        public Sequence(long initialValue) {
            super(initialValue);
        }
    }
}
