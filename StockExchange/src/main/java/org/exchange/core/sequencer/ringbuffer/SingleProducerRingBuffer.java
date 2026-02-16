//package org.exchange.core.sequencer.ringbuffer;
//
//import org.exchange.core.util.data.Order;
//
//import java.util.concurrent.atomic.AtomicLong;
//
//public class SingleProducerRingBuffer implements RingBuffer {
//    private final int ringBufferLength;
//    private final Order[] ringBuffer;
//
//    private final Sequence producerOffset = new Sequence(0);
//    private final Sequence[] consumerOffsets;
//    private final Sequence minConsumerOffset = new Sequence(0);
//
//    private long cachedMinConsumerOffset;
//
//    public SingleProducerRingBuffer(int numConsumers) {
//        this.ringBufferLength = 65536;
//        this.ringBuffer = new Order[ringBufferLength];
//        for (int i = 0; i < ringBufferLength; i++) {
//            ringBuffer[i] = new Order(-1, 0, -1);
//        }
//        this.consumerOffsets = new Sequence[numConsumers];
//        for (int i = 0; i < numConsumers; i++) {
//            consumerOffsets[i] = new Sequence(0);
//        }
//    }
//
//    public SingleProducerRingBuffer(int numConsumers, int bufferLength) throws Exception {
//        if (bufferLength < 1 || (bufferLength & (bufferLength - 1)) != 0) {
//            throw new Exception("Ring Buffer Length should be a power of 2");
//        }
//        this.ringBufferLength = bufferLength;
//        this.ringBuffer = new Order[ringBufferLength];
//        for (int i = 0; i < ringBufferLength; i++) {
//            ringBuffer[i] = new Order();
//        }
//        this.consumerOffsets = new Sequence[numConsumers];
//        for (int i = 0; i < numConsumers; i++) {
//            consumerOffsets[i] = new Sequence(0);
//        }
//    }
//
//
//    public void addOrder(int value) {
//        long currentOffset = producerOffset.get();
//
//        if (currentOffset - cachedMinConsumerOffset >= ringBufferLength) {
//            cachedMinConsumerOffset = getMinimumConsumerOffset();
//            while (currentOffset - cachedMinConsumerOffset >= ringBufferLength) {
//                Thread.onSpinWait();
//                cachedMinConsumerOffset = getMinimumConsumerOffset();
//            }
//        }
//
//
//        ringBuffer[(int) (currentOffset & (ringBuffer.length - 1))].setValue(value);
//        producerOffset.lazySet(currentOffset + 1);
//    }
//
//    public int readOrder(int consumerId) {
//        Sequence consumerSequence = consumerOffsets[consumerId];
//        long currentOffset = consumerSequence.get();
//
//        // Wait for the producer to provide data
//        while (currentOffset >= producerOffset.get()) {
//            Thread.onSpinWait();
//        }
//
//        int value = ringBuffer[(int) (currentOffset & (ringBufferLength - 1))].getValue();
//
//        long nextOffset = currentOffset + 1;
//        consumerSequence.lazySet(nextOffset);
//
//        // Batch the update to the shared minimum to avoid hitting the producer's cache line too often
//        if ((nextOffset & 0x7F) == 0) {
//            updateMinConsumerOffset(nextOffset);
//        }
//
//        return value;
//    }
//
//    private void updateMinConsumerOffset(long current) {
//        if (consumerOffsets.length == 1) {
//            minConsumerOffset.lazySet(current);
//        } else {
//            long min = current;
//            for (Sequence s : consumerOffsets) {
//                min = Math.min(min, s.get());
//            }
//            minConsumerOffset.lazySet(min);
//        }
//    }
//
//    private long getMinimumConsumerOffset() {
//        return minConsumerOffset.get();
//    }
//
//    public long getProducerOffset() {
//        return producerOffset.get();
//    }
//
//    private static class Sequence extends AtomicLong {
//        public long p1, p2, p3, p4, p5, p6, p7;
//        public Sequence(long initialValue) {
//            super(initialValue);
//        }
//    }
//}
