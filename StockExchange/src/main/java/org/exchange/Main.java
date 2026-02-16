package org.exchange;

import org.exchange.core.matching.PriceMap;
import org.exchange.core.sequencer.ringbuffer.MultiProducerRingBuffer;
import org.exchange.ingress.TcpOrderIngress;
import org.exchange.monitor.ExchangeMonitor;

import java.util.concurrent.atomic.LongAdder;

public class Main {
    // High-performance counter for metrics
    private static final LongAdder processedCount = new LongAdder();

    public static void main(String[] args) {
        MultiProducerRingBuffer ringBuffer = new MultiProducerRingBuffer(1);
        PriceMap matchingEngine = new PriceMap();
        ExchangeMonitor exchangeMonitor = new ExchangeMonitor();

        Thread monitorThread = new Thread(exchangeMonitor.monitor(processedCount));
        monitorThread.start();

        Thread matchingEngineThread = new Thread(matchingEngine.run(ringBuffer));
        matchingEngineThread.start();

        TcpOrderIngress tcpOrderIngress = new TcpOrderIngress(8080, ringBuffer);
        tcpOrderIngress.start();
    }
}