package org.exchange.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TcpOrderClient implements AutoCloseable {
    private final String host;
    private final int orderPort;
    private final int pricePort;
    private SocketChannel orderClient;
    private SocketChannel priceClient;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
    private final ByteBuffer priceBuffer = ByteBuffer.allocateDirect(8);

    public TcpOrderClient(String host, int orderPort, int pricePort) {
        this.host = host;
        this.orderPort = orderPort;
        this.pricePort = pricePort;
    }

    public void connect() throws IOException {
        orderClient = SocketChannel.open(new InetSocketAddress(host, orderPort));
        orderClient.configureBlocking(true);

        priceClient = SocketChannel.open(new InetSocketAddress(host, pricePort));
        priceClient.configureBlocking(true);

        System.out.println("Connected to Order Ingress at " + host + ":" + orderPort);
        System.out.println("Connected to Price Ingress at " + host + ":" + pricePort);
    }

    public void sendOrder(long qty, long price, boolean isBuy) throws IOException {
        buffer.clear();
        buffer.putLong(qty);
        buffer.putLong(price);
        buffer.put((byte) (isBuy ? 1 : 0));
        buffer.flip();

        while (buffer.hasRemaining()) {
            orderClient.write(buffer);
        }
    }

    @Override
    public void close() throws IOException {
        if (orderClient != null) {
            orderClient.close();
        }

        if (priceClient != null) {
            priceClient.close();
        }
    }

    public static void main(String[] args) {
        try (TcpOrderClient apiClient = new TcpOrderClient("localhost", 9090, 9091)) {
            apiClient.connect();
            // Start listening for commands from Electron/Stdin
            apiClient.startOrderListener();
            apiClient.startPricePinger();

            // Keep the main thread alive while the listener thread runs
            Thread.currentThread().join();
        } catch (Exception e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    public void startOrderListener() {
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

    public void startPricePinger() {
        Thread pingerThread = new Thread(() -> {
            ByteBuffer ping = ByteBuffer.allocate(1);
            ping.put((byte) 1);
        
            byte[] outboundPrice = new byte[8];
            ByteBuffer outboundBuffer = ByteBuffer.wrap(outboundPrice);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ping.clear();
                    ping.put((byte) 1);
                    ping.flip();
                    
                    // Ensure the ping is sent
                    while(ping.hasRemaining()) {
                        priceClient.write(ping);
                    }

                    priceBuffer.clear();
                    // Blocking read for exactly 8 bytes
                    int totalRead = 0;
                    while (totalRead < 8) {
                        int read = priceClient.read(priceBuffer);
                        if (read == -1) throw new IOException("Price server closed connection");
                        totalRead += read;
                    }
                    
                    priceBuffer.flip();
                    long lastPrice = priceBuffer.getLong();
                
                    outboundBuffer.clear();
                    outboundBuffer.putLong(lastPrice);
                    System.out.write(outboundPrice);
                    System.out.flush(); 

                    Thread.sleep(1000);
                } catch (IOException | InterruptedException e) {
                    System.err.println("Price pinger error: " + e.getMessage());
                    break;
                }
            }
        }, "PricePinger");
        pingerThread.setDaemon(true);
        pingerThread.start();
    }
}