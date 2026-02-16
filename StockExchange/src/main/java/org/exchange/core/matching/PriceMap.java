package org.exchange.core.matching;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;

import org.exchange.core.sequencer.ringbuffer.RingBuffer;
import org.exchange.core.util.Order;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class PriceMap {
    private final Long2ObjectRBTreeMap<PriceLevel> bids = new Long2ObjectRBTreeMap<>( (a, b) -> Long.compare(b, a) ); // Descending
    private final Long2ObjectRBTreeMap<PriceLevel> asks = new Long2ObjectRBTreeMap<>( Long::compare ); // Ascending
    private final LongAdder processedCount;

    // Cached "Top of Book" values
    private long bestBidPrice = 0;
    private long bestAskPrice = Long.MAX_VALUE;

    public volatile long lastProcessedPrice;

    public PriceMap(LongAdder processedCount) {
        this.processedCount = processedCount;
    }

    public Runnable run(RingBuffer ringBuffer) {
        return () -> {
            try {
                System.out.println("Matching Engine started...");
                while (true) {
                    Order incomingOrder = ringBuffer.readOrder(0);
                    processOrder(incomingOrder, incomingOrder.isBuy);
                    processedCount.increment();
                }
            } catch (Throwable t) {
                System.err.println("Matching Engine CRASHED:");
                t.printStackTrace();
            }
        };
    }

    public Runnable returnMarketPrice() {
        return () -> {
            try {
                System.out.println("Market Data Provider started...");
                while (true) {
                    Thread.sleep(1000);
                    System.out.println("Market Data Provider: " + lastProcessedPrice);
                }
            } catch (Throwable t) {
                System.err.println("Market Data Provider CRASHED:");
            }
        };
    }

    public void processOrder(Order incomingOrder, boolean isBuy) {
        long remainingQty = incomingOrder.qty;

        while (remainingQty > 0) {
            long bestOppositePrice = isBuy ? bestAskPrice : bestBidPrice;

            boolean canMatch = isBuy ? (incomingOrder.price >= bestOppositePrice)
                    : (incomingOrder.price <= bestOppositePrice);

            if (!canMatch || bestOppositePrice == 0 || bestOppositePrice == Long.MAX_VALUE) {
                break;
            }

            PriceLevel bestLevel = (isBuy ? asks : bids).get(bestOppositePrice);

            // FIX 1: Null safety check to prevent the crash you saw
            if (bestLevel == null || bestLevel.isEmpty()) {
                refreshTopOfBook(isBuy);
                continue;
            }

            Order makerOrder = bestLevel.getOrders().head;

            // FIX 2: Defensive check against corrupted/overwritten RingBuffer objects
            if (makerOrder == null || makerOrder.qty <= 0) {
                bestLevel.getOrders().removeHead();
                if (bestLevel.isEmpty()) {
                    (isBuy ? asks : bids).remove(bestOppositePrice);
                    refreshTopOfBook(isBuy);
                }
                continue;
            }

            lastProcessedPrice = makerOrder.price;

            long matchQty = Math.min(remainingQty, makerOrder.qty);

            // Update quantities
            remainingQty -= matchQty;
            makerOrder.qty -= matchQty;

            if (makerOrder.qty == 0) {
                removeOrder(makerOrder, !isBuy);
            }
        }

        // FIX 3: THE MOST IMPORTANT FIX - Defensive Copying
        // Do not add the 'incomingOrder' (RingBuffer reference) to the book.
        // Create a new instance that the Producers cannot overwrite.
        if (remainingQty > 0) {
            Order restingOrder = new Order(
                    incomingOrder.orderId,
                    remainingQty,
                    incomingOrder.price,
                    isBuy
            );
            addOrder(restingOrder, isBuy);
        }
    }

    public void addOrder(Order order, boolean isBuy) {
        Long2ObjectRBTreeMap<PriceLevel> targetMap = isBuy ? bids : asks;

        // 1. Find or create the Price Level
        PriceLevel level = targetMap.get(order.price);
        if (level == null) {
            level = new PriceLevel(order.price);
            targetMap.put(order.price, level);

            updateTopOfBook(order.price, isBuy);
        }

        // 2. Add the order to the FIFO queue
        level.addOrder(order);
    }

    public void removeOrder(Order order, boolean isBuy) {
        Long2ObjectRBTreeMap<PriceLevel> targetMap = isBuy ? bids : asks;

        // 1. Find or create the Price Level
        PriceLevel level = targetMap.get(order.price);
        if (level == null) {
            return;
        }

        // 2. Add the order to the FIFO queue
        level.removeOrder(order);

        // 3. Remove the level if no orders are left
        if (level.isEmpty()) {
            targetMap.remove(order.price);
            refreshTopOfBook(isBuy);
        }
    }

    private void updateTopOfBook(long price, boolean isBuy) {
        if (isBuy) {
            if (price > bestBidPrice) bestBidPrice = price;
        } else {
            if (price < bestAskPrice) bestAskPrice = price;
        }
    }

    private void refreshTopOfBook(boolean isBuy) {
        if (isBuy) {
            bestBidPrice = bids.isEmpty() ? 0 : bids.firstLongKey();
        } else {
            bestAskPrice = asks.isEmpty() ? Long.MAX_VALUE : asks.firstLongKey();
        }
    }

    public long getBestBid() { return bestBidPrice; }
    public long getBestAsk() { return bestAskPrice; }
}
