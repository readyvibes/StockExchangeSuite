# Stock Exchange Suite

A high-performance, full-stack electronic trading system featuring a lock-free matching engine, a binary TCP gateway. 
This suite is designed for low-latency execution, utilizing a multi-producer ring buffer to achieve throughputs sometimes exceeding 1 million orders per second.

## TCP Order Ingress Workflow
<p>
  <img src="./docs/TCP Order Ingress Workflow.png" alt="Architecture Diagram" width="900">
</p>

System Architecture
The platform follows a modular architecture designed to isolate network handling from core matching logic:
1. Network Ingress: A non-blocking Java NIO Selector manages concurrent client connections on port 8080, deserializing raw binary streams into order objects.
2. Sequencer: Orders are dispatched into a 2,097,152-slot MultiProducerRingBuffer. This lock-free structure utilizes memory padding and AtomicLong offsets to eliminate contention between threads.
3. Matching Engine: A dedicated thread processes the RingBuffer stream, executing trades via a PriceMap that maintains price-time priority using Long2ObjectRBTreeMap and custom doubly-linked lists.


## Setup (Try It Out)
1. Compile and run the project with `mvn clean install`
2. Execute 'StockExchange' org.exchange.Main to start the Ingress (Port 8080), Engine, and Metrics Monitor


