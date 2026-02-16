//package org.exchange.core.sequencer.benchmark;
//
//import org.exchange.core.sequencer.ringbuffer.SingleProducerRingBuffer;
//import org.exchange.core.sequencer.ringbuffer.MultiProducerRingBuffer;
//
//
//import java.util.concurrent.CountDownLatch;
//
//public class Benchmark {
//
//    // Number of messages to send
//    private static final int ITERATIONS = 50_000_000;
//    // Buffer size (Must be power of 2, e.g., 1024 * 64)
//    private static final int BUFFER_SIZE = 2097152;
//
//    public static void testSingleProducer(int NUM_CONSUMERS) {
//        // Initialize SingleProducerRingBuffer
//        try {
//            SingleProducerRingBuffer singleProducerRingBuffer = new SingleProducerRingBuffer(1, BUFFER_SIZE);
//
//            // We use a Latch to wait for the consumer to finish before stopping the timer
//            CountDownLatch latch = new CountDownLatch(1);
//
//            final Exception[] threadError = {null};
//
//            for (int i = 0; i < NUM_CONSUMERS; i++) {
//                final int consumerId = i;
//                new Thread(() -> {
//                    try {
//                        for (int j = 0; j < ITERATIONS; j++) {
//                            int value = singleProducerRingBuffer.readOrder(consumerId);
//                            if (value != 1) {
//                                throw new RuntimeException("Consumer " + consumerId + ": Order corruption! Expected 1 but got " + value);
//                            }
//                        }
//                    } catch (Exception e) {
//                        synchronized (threadError) {
//                            if (threadError[0] == null) {
//                                threadError[0] = e;
//                            }
//                        }
//                    } finally {
//                        latch.countDown();
//                    }
//                }).start();
//            }
//
//            System.out.println("Starting Benchmark for " + ITERATIONS + " messages...");
//
//            long startTime = System.nanoTime();
//
//            for (int i = 0; i < ITERATIONS; i++) {
//                singleProducerRingBuffer.addOrder(1);
//            }
//
//            // Wait for consumer to finish processing everything
//            latch.await();
//
//            long endTime = System.nanoTime();
//            long totalTimeNs = endTime - startTime;
//
//            printResults(totalTimeNs);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    public static void testMultiProducer(int NUM_PRODUCERS, int NUM_CONSUMERS) {
//        try {
//            MultiProducerRingBuffer multiProducerRingBuffer = new MultiProducerRingBuffer(NUM_CONSUMERS, 2097152);
//
//            System.out.println("Starting Benchmark for " + ITERATIONS * NUM_PRODUCERS + " messages...");
//
//            // Wait for all producers and consumers to finish
//            CountDownLatch latch = new CountDownLatch(NUM_PRODUCERS + NUM_CONSUMERS);
//            CountDownLatch startLatch = new CountDownLatch(1);
//
//            // Start Consumers
//            for (int i = 0; i < NUM_CONSUMERS; i++) {
//                final int consumerId = i;
//                new Thread(() -> {
//                    try {
//                        startLatch.await();
//                        for (int j = 0; j < (long) ITERATIONS * NUM_PRODUCERS; j++) {
//                            multiProducerRingBuffer.readOrder(consumerId);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        latch.countDown();
//                    }
//                }).start();
//            }
//
//            // Start Producers
//            for (int i = 0; i < NUM_PRODUCERS; i++) {
//                new Thread(() -> {
//                    try {
//                        startLatch.await();
//                        for (int j = 0; j < ITERATIONS; j++) {
//                            multiProducerRingBuffer.addOrder(1);
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    } finally {
//                        latch.countDown();
//                    }
//                }).start();
//            }
//
//            System.out.printf("Starting Multi-Producer Benchmark (%d producers, %d consumers)...%n", NUM_PRODUCERS, NUM_CONSUMERS);
//            long startTime = System.nanoTime();
//            startLatch.countDown();
//
//            latch.await();
//            long endTime = System.nanoTime();
//
//            long totalTimeNs = endTime - startTime;
//            // Total operations = iterations per producer * number of producers
//            printResults(totalTimeNs, (long) ITERATIONS * NUM_PRODUCERS);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private static void printResults(long totalTimeNs) {
//        printResults(totalTimeNs, ITERATIONS);
//    }
//
//    private static void printResults(long totalTimeNs, long iterations) {
//        double totalTimeSeconds = totalTimeNs / 1_000_000_000.0;
//        long opsPerSecond = (long) (iterations / totalTimeSeconds);
//        double latencyNs = (double) totalTimeNs / iterations;
//
//        System.out.printf("Total Time:     %.4f seconds%n", totalTimeSeconds);
//        System.out.printf("Throughput:     %,d ops/sec%n", opsPerSecond);
//        System.out.printf("Avg Latency:    %.2f ns/op%n", latencyNs);
//        System.out.println("--------------------------------");
//        if (latencyNs < 1000) {
//            System.out.println("RESULT: Sub-microsecond latency confirmed! (Nanosecond scale)");
//        } else {
//            System.out.println("RESULT: Slower than 1 microsecond.");
//        }
//    }
//}
