package org.exchange.ingress;

import org.exchange.core.sequencer.ringbuffer.MultiProducerRingBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class TcpOrderIngress implements Runnable{
    private final MultiProducerRingBuffer ringBuffer;
    private final int port;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    private Selector selector;
    private ServerSocketChannel serverChannel;


    public TcpOrderIngress(int port, MultiProducerRingBuffer ringBuffer) {
        this.ringBuffer = ringBuffer;
        this.port = port;
    }

    public void start() {
        try {
            // 1. Opens the 'Selector,' which acts as a multiplexer to monitor multiple connections on a single thread.
            selector = Selector.open();

            // 2. Creates the initial 'ServerSocketChannel' that will listen for new incoming trading connections.
            serverChannel = ServerSocketChannel.open();

            // 3. Binds the channel to a specific IP and port (e.g., 9090) to start listening for client traffic.
            serverChannel.bind(new InetSocketAddress(port));

            // 4. Sets the channel to non-blocking mode so the thread doesn't hang while waiting for a client to connect.
            serverChannel.configureBlocking(false);

            // 5. Registers this channel with the selector to notify the 'run' loop only when a new client wants to 'ACCEPT'.
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            new Thread(this, "Ingress-Gateway-Thread").start(); // Calls run method because "this"
            System.out.println("Ingress Gateway listening on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
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

                    if (key.isAcceptable()) {
                        handleAccept();
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        System.out.println("Client connected: " + client.getRemoteAddress());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
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
