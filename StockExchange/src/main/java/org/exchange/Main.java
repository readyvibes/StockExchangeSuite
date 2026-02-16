package org.exchange;

import org.exchange.core.matching.PriceMap;
import org.exchange.core.sequencer.ringbuffer.MultiProducerRingBuffer;
import org.exchange.ingress.TcpOrderIngress;
import org.exchange.monitor.ExchangeMonitor;

import java.util.concurrent.atomic.LongAdder;

public class Main {
    // High-performance counter for metrics


    public static void main(String[] args) {
        TcpOrderIngress tcpOrderIngress = new TcpOrderIngress();
        tcpOrderIngress.start();
    }
}