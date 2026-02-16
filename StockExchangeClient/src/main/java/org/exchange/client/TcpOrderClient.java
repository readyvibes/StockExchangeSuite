package org.exchange.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public void sendOrder(long qty, long price, boolean isBuy) throws IOException {
        buffer.clear();
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
            System.out.println("Java Client: Listening for BINARY commands on stdin...");
            try {
                java.io.InputStream in = System.in;
                byte[] messageBuffer = new byte[17]; // 8 (qty) + 8 (price) + 1 (isBuy)
                
                while (!Thread.currentThread().isInterrupted()) {
                    int bytesRead = 0;
                    // Ensure we read a full 17-byte message
                    while (bytesRead < 17) {
                        int result = in.read(messageBuffer, bytesRead, 17 - bytesRead);
                        if (result == -1) return; // Stream closed
                        bytesRead += result;
                    }

                    // Wrap the bytes into a ByteBuffer to extract longs without string parsing
                    ByteBuffer bb = ByteBuffer.wrap(messageBuffer);
                    long qty = bb.getLong();
                    long price = bb.getLong();
                    boolean isBuy = bb.get() == 1;

                    sendOrder(qty, price, isBuy);
                }
            } catch (IOException e) {
                System.err.println("Binary command listener error: " + e.getMessage());
            }
        }, "ElectronBinaryListener");

        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}