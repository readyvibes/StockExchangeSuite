package org.exchange.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

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
            // Start listening for commands from Electron/Stdin
            apiClient.startCommandListener();

            // Keep the main thread alive while the listener thread runs
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    public void startCommandListener() {
        Thread listenerThread = new Thread(() -> {
            System.out.println("Java Client: Listening for commands on stdin...");
            try (Scanner scanner = new Scanner(System.in)) {
                while (!Thread.currentThread().isInterrupted() && scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.isEmpty()) continue;

                    try {
                        // Expected format: SEND_ORDER,orderId,qty,price,isBuy
                        String[] parts = line.split(",");
                        if ("SEND_ORDER".equals(parts[0]) && parts.length == 5) {
                            long id = Long.parseLong(parts[1]);
                            long qty = Long.parseLong(parts[2]);
                            long price = Long.parseLong(parts[3]);
                            boolean isBuy = Boolean.parseBoolean(parts[4]);

                            sendOrder(id, qty, price, isBuy);
                            System.out.println("Sent order: " + id);
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to parse command: " + line + " | Error: " + e.getMessage());
                    }
                }
            }
        }, "ElectronCommandListener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}