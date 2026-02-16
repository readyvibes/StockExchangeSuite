package org.exchange.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TcpOrderClient implements AutoCloseable {
    private final String host;
    private final int port;
    private SocketChannel client;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

    public TcpOrderClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        client = SocketChannel.open(new InetSocketAddress(host, port));
        client.configureBlocking(true);
        System.out.println("Connected to Exchange at " + host + ":" + port);
    }

    public void sendOrder(long orderId, long qty, long price, boolean isBuy) throws IOException {
        buffer.clear();
        buffer.putLong(orderId);
        buffer.putLong(qty);
        buffer.putLong(price);
        buffer.put((byte) (isBuy ? 1 : 0));
        buffer.flip();

        while (buffer.hasRemaining()) {
            client.write(buffer);
        }
    }

    public void sendBatch(int count) throws IOException {
        buffer.clear();
        for (int i = 0; i < count; i++) {
            if (buffer.remaining() < 25) {
                buffer.flip();
                client.write(buffer);
                buffer.clear();
            }
            buffer.putLong(System.nanoTime()); // Using time as dummy ID
            buffer.putLong(100);               // Qty
            buffer.putLong(500);               // Price
            buffer.put((byte) 1);              // Buy
        }
        buffer.flip();
        while (buffer.hasRemaining()) {
            client.write(buffer);
        }
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }

    public static void main(String[] args) {
        try (TcpOrderClient apiClient = new TcpOrderClient("localhost", 8080)) {
            apiClient.connect();

            System.out.println("Sending 10,000 test orders...");
            for (int i = 0; i < 10000; i++) {
                apiClient.sendOrder(i, 10, 100 + i, true);
            }
            System.out.println("Orders sent successfully.");

        } catch (IOException e) {
            System.err.println("Failed to connect or send orders: " + e.getMessage());
        }
    }
}