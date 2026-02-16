package org.exchange.monitor;

import java.util.concurrent.atomic.LongAdder;

public class ExchangeMonitor {

    public ExchangeMonitor() {}

    public Runnable monitor(LongAdder processedCount) {
        return () -> {
            try {
                while (true) {
                    Thread.sleep(1000);
                    long count = processedCount.sumThenReset();
                    if (count > 0) {
                        System.out.printf("[METRICS] Throughput: %,d orders/sec%n", count);
                    } else {
                        System.out.println("[METRICS] Waiting for orders...");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }
}
