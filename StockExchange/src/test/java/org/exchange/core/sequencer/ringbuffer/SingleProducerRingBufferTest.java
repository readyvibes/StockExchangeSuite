//package org.exchange.core.sequencer.ringbuffer;
//
//import org.junit.Test;
//
//import java.util.concurrent.CountDownLatch;
//
//import static org.junit.Assert.*;
//
//public class SingleProducerRingBufferTest {
//
//    @Test
//    public void testConsumerOnlyConstructor() {
//        SingleProducerRingBuffer rb = new SingleProducerRingBuffer(1); // or whatever your default logic is
//        assertNotNull(rb);
//    }
//
//    @Test
//    public void testConsumerBufferLengthConstructor() throws Exception {
//        SingleProducerRingBuffer rb = new SingleProducerRingBuffer(1, 16);
//        assertNotNull(rb);
//    }
//
//    @Test(expected = Exception.class)
//    public void testConsumerBufferLengthException() throws Exception {
//        SingleProducerRingBuffer rb = new SingleProducerRingBuffer(1, 19);
//    }
//
//    @Test
//    public void testRingBufferSuccess() throws Exception { // Allow test to throw exceptions
//        int ITERATIONS = 50_000_000;
//        int BUFFER_SIZE = 65536;
//
//        // Use an array or AtomicReference to hold any error from the thread
//        final Exception[] threadError = {null};
//
//        SingleProducerRingBuffer singleProducerRingBuffer = new SingleProducerRingBuffer(1, BUFFER_SIZE);
//        CountDownLatch latch = new CountDownLatch(1);
//
//        Thread consumer = new Thread(() -> {
//            try {
//                for (int i = 0; i < ITERATIONS; i++) {
//                    int value = singleProducerRingBuffer.readOrder(0);
//                    if (value != 1) throw new RuntimeException("Order corruption! Expected 1 but got " + value);
//                }
//            } catch (Exception e) {
//                threadError[0] = e; // Capture the error
//            } finally {
//                latch.countDown();
//            }
//        });
//
//        consumer.start();
//
//        for (int i = 0; i < ITERATIONS; i++) {
//            singleProducerRingBuffer.addOrder(1);
//        }
//
//        latch.await();
//
//        // ASSERTION: Fail the test if the consumer thread caught an exception
//        if (threadError[0] != null) {
//            throw threadError[0];
//        }
//
//        assertEquals("", ITERATIONS, singleProducerRingBuffer.getProducerOffset());
//    }
//
//    @Test
//    public void testMultipleConsumersSuccess() throws Exception {
//        int ITERATIONS = 50_000_000;
//        int BUFFER_SIZE = 65536;
//        int NUM_CONSUMERS = 4;
//
//        final Exception[] threadError = {null};
//        // Initialize buffer with multiple consumers
//        SingleProducerRingBuffer ringBuffer = new SingleProducerRingBuffer(NUM_CONSUMERS, BUFFER_SIZE);
//        CountDownLatch latch = new CountDownLatch(NUM_CONSUMERS);
//
//        // Start multiple consumer threads
//        for (int i = 0; i < NUM_CONSUMERS; i++) {
//            final int consumerId = i;
//            new Thread(() -> {
//                try {
//                    for (int j = 0; j < ITERATIONS; j++) {
//                        int value = ringBuffer.readOrder(consumerId);
//                        if (value != 1) {
//                            throw new RuntimeException("Consumer " + consumerId + ": Order corruption! Expected 1 but got " + value);
//                        }
//                    }
//                } catch (Exception e) {
//                    synchronized (threadError) {
//                        if (threadError[0] == null) {
//                            threadError[0] = e;
//                        }
//                    }
//                } finally {
//                    latch.countDown();
//                }
//            }).start();
//        }
//
//        // Producer adds orders
//        for (int i = 0; i < ITERATIONS; i++) {
//            ringBuffer.addOrder(1);
//        }
//
//        latch.await();
//
//        if (threadError[0] != null) {
//            throw threadError[0];
//        }
//
//        assertEquals(ITERATIONS, ringBuffer.getProducerOffset());
//    }
//}
