# StockExchange Project Structure & Guide

This document provides a high-level overview of the files in the `StockExchange` project and explains which files to modify when making specific changes.

## Core Execution
### `Main.java`
*   **Purpose:** The entry point of the application. It bootstraps the system and orchestrates the flow between the sequencer and the matching engine.
*   **When to change:** If you need to add new command-line arguments, initialize new components, or change the high-level startup logic.

---

## Matching Engine (`org.exchange.core.matching`)
### `PriceMap.java`
*   **Purpose:** The central logic of the exchange. It maintains the Limit Order Book (Bids and Asks) and handles the "matching" logic when a new order arrives.
*   **When to change:** If you want to modify **how orders match** (e.g., changing from Price-Time priority), or if you need to update how "Top of Book" prices are tracked.

### `PriceLevel.java`
*   **Purpose:** Represents a single price point in the book. It acts as a container for all orders at that price and tracks the total volume (quantity) available.
*   **When to change:** If you need to add more metadata to a specific price point, like the number of unique orders or historical volume.

### `OrderLinkedList.java` (util)
*   **Purpose:** A custom, high-performance Doubly Linked List designed for `Order` objects. It ensures that adding or removing an order is an $O(1)$ operation.
*   **When to change:** If you need to optimize the memory footprint of the order queues or fix issues related to how orders are linked together.

---

## Sequencer & Buffering (`org.exchange.core.sequencer`)
### `RingBuffer.java`
*   **Purpose:** An interface defining the contract for high-speed message passing.
*   **When to change:** If you need to define new shared behavior for different types of buffers.

### `SingleProducerRingBuffer.java`
*   **Purpose:** A low-latency, lock-free buffer optimized for a single producer and multiple consumers. It ensures data is passed between threads with minimal overhead.
*   **When to change:** If you need to tune **concurrency performance**, change how threads wait for data (spin-waiting), or adjust the buffer's memory padding.

---

## Data Models & Utils (`org.exchange.core.util`)
### `Order.java`
*   **Purpose:** The fundamental data structure representing a trade request (ID, price, quantity, side).
*   **When to change:** If you need to add new fields to an order, such as a **timestamp**, **Order Type** (Market vs. Limit), or **Trader ID**.

---

## Testing & Performance
### `SingleProducerRingBufferTest.java`
*   **Purpose:** Unit tests for the RingBuffer to ensure thread safety and data integrity.
*   **When to change:** Whenever you modify the RingBuffer logic to ensure no regressions are introduced.

### `Benchmark.java`
*   **Purpose:** A tool to measure the throughput (ops/sec) and latency (ns/op) of the exchange components.
*   **When to change:** If you want to test the system under different loads, such as increasing the number of concurrent traders (producers).

---

## Quick Reference: Which file do I change?

| If you want to... | Go to... |
| :--- | :--- |
| **Change how trades are matched** | `PriceMap.java` |
| **Add a "Timestamp" to every order** | `Order.java` |
| **Increase the speed of thread communication** | `SingleProducerRingBuffer.java` |
| **Track total volume at a price** | `PriceLevel.java` |
| **Fix FIFO (First-In-First-Out) order issues** | `OrderLinkedList.java` |
| **Add a new startup configuration** | `Main.java` |