package org.exchange.ingress;

import org.exchange.core.matching.PriceMap;
import org.exchange.core.sequencer.ringbuffer.MultiProducerRingBuffer;
import org.exchange.monitor.ExchangeMonitor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class TcpOrderIngress implements Runnable{
    private final MultiProducerRingBuffer ringBuffer;
    private final PriceMap priceMap;
    private final ExchangeMonitor exchangeMonitor;
    private static final LongAdder processedCount = new LongAdder();
    private final int orderPort = 9090;
    private final int pricePort = 9091;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    private final ByteBuffer priceOutputBuffer = ByteBuffer.allocateDirect(8);
    private Selector selector;
    private ServerSocketChannel orderServerChannel;
    private ServerSocketChannel priceServerChannel;

    private final Set<SocketChannel> priceSubscribers = ConcurrentHashMap.newKeySet();

    public TcpOrderIngress() {
        ringBuffer = new MultiProducerRingBuffer(1);
        priceMap = new PriceMap(processedCount);
        exchangeMonitor = new ExchangeMonitor();
    }

    public void start() {
        try {
            Thread monitorThread = new Thread(exchangeMonitor.monitor(processedCount));
            monitorThread.start();

            Thread matchingEngineThread = new Thread(priceMap.run(ringBuffer));
            matchingEngineThread.start();

//            Thread marketDataProvider = new Thread(priceMap.returnMarketPrice());
//            marketDataProvider.start();

            selector = Selector.open();

            orderServerChannel = ServerSocketChannel.open();
            orderServerChannel.bind(new InetSocketAddress(orderPort));
            orderServerChannel.configureBlocking(false);
            orderServerChannel.register(selector, SelectionKey.OP_ACCEPT);

            priceServerChannel = ServerSocketChannel.open();
            priceServerChannel.bind(new InetSocketAddress(pricePort));
            priceServerChannel.configureBlocking(false);
            priceServerChannel.register(selector, SelectionKey.OP_ACCEPT);

            new Thread(this, "Ingress-Gateway-Thread").start(); // Calls run method because "this"
            System.out.println("Order Ingress Gateway listening on port " + orderPort);
            System.out.println("Price Ingress Gateway listening on port " + pricePort);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isValid() && key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    private void handleAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) selectionKey.channel();
        SocketChannel client = server.accept();

        if (client != null) {
            client.configureBlocking(false);

            // Attach the port to the key so handleRead knows which logic to use
            int port = ((InetSocketAddress)server.getLocalAddress()).getPort();
            client.register(selector, SelectionKey.OP_READ, port);

            if (server == priceServerChannel) {
                System.out.println("Price Query Client connected: " + client.getRemoteAddress());
            } else {
                System.out.println("Order Client connected: " + client.getRemoteAddress());
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Integer port = (Integer) key.attachment();

        // Use the attachment to route the request
        if (port != null && port == pricePort) {
            handlePriceRequest(client);
        } else {
            handleOrderRequest(client);
        }
    }

    private void handlePriceRequest(SocketChannel client) throws IOException {
        ByteBuffer pingBuffer = ByteBuffer.allocate(1);
        int read = client.read(pingBuffer);

        if (read == -1) {
            client.close();
            return;
        }

        if (read > 0) {
            // User sent a ping, respond with the latest price
            priceOutputBuffer.clear();
            priceOutputBuffer.putLong(priceMap.lastProcessedPrice);
            System.out.println("Last Processed Price: " + priceMap.lastProcessedPrice);
            priceOutputBuffer.flip();
            client.write(priceOutputBuffer);
        }
    }

    private void handleOrderRequest(SocketChannel client) throws IOException {
        int bytesRead = client.read(buffer);

        if (bytesRead == -1) {
            client.close();
            return;
        }

        buffer.flip();
        // Assume a simple binary protocol:
        // Long (8) qty, Long (8) price, Byte (1) isBuy
        while (buffer.remaining() >= 17) {
            long qty = buffer.getLong();
            long price = buffer.getLong();
            boolean isBuy = buffer.get() == 1;

            // Push directly to the high-performance ring buffer
            ringBuffer.addOrder(qty, price, isBuy);
        }

        buffer.compact();
    }
}
