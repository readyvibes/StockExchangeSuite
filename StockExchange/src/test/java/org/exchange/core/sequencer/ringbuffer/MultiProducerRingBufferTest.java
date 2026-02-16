package org.exchange.core.sequencer.ringbuffer;

import org.exchange.core.util.Order;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class MultiProducerRingBufferTest {

    @Test
    public void testConcurrentProducersAndConsumers() throws Exception {
        int NUM_PRODUCERS = 4;
        int NUM_CONSUMERS = 2;
        int ITEMS_PER_PRODUCER = 1_000_000;
        int TOTAL_ITEMS = NUM_PRODUCERS * ITEMS_PER_PRODUCER;

        MultiProducerRingBuffer ringBuffer = new MultiProducerRingBuffer(NUM_CONSUMERS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(NUM_PRODUCERS + NUM_CONSUMERS);

        // Track the sum of all qty values read by each consumer to verify integrity
        AtomicLong[] consumerSums = new AtomicLong[NUM_CONSUMERS];
        for (int i = 0; i < NUM_CONSUMERS; i++) consumerSums[i] = new AtomicLong(0);

        final Exception[] error = {null};

        // 1. Start Consumer Threads
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < TOTAL_ITEMS; j++) {
                        Order val = ringBuffer.readOrder(id);
                        consumerSums[id].addAndGet(val.qty);
                    }
                } catch (Exception e) {
                    error[0] = e;
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        // 2. Start Producer Threads
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                        // Each producer sends an order with qty 1
                        ringBuffer.addOrder(1, 100, true);
                    }
                } catch (Exception e) {
                    error[0] = e;
                } finally {
                    endLatch.countDown();
                }
            }).start();
        }

        // Start the race
        startLatch.countDown();
        endLatch.await();

        if (error[0] != null) throw error[0];

        // 3. Verify results
        // Total sum should be TOTAL_ITEMS (since every producer sent qty=1)
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            assertEquals("Consumer " + i + " missed data", (long) TOTAL_ITEMS, consumerSums[i].get());
        }
    }

    @Test(expected = Exception.class)
    public void testInvalidBufferSize() throws Exception {
        new MultiProducerRingBuffer(1, 15); // Not a power of 2
    }

    @Test
    public void testSingleProducerSingleConsumer() throws Exception {
        int ITERATIONS = 1000;
        MultiProducerRingBuffer rb = new MultiProducerRingBuffer(1, 65536);
        CountDownLatch latch = new CountDownLatch(1);

        // Start a consumer thread so the producer doesn't get stuck when the buffer is full
        new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                rb.readOrder(0);
            }
            latch.countDown();
        }).start();

        for (int i = 0; i < ITERATIONS; i++) {
            rb.addOrder(1, 100, true);
        }

        latch.await(); // Wait for consumer to finish
        assertEquals(1000, rb.getProducerOffset());
    }
}