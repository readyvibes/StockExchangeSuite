package org.exchange;

import org.exchange.client.TcpOrderClient;

public class Main {
    public static void main(String[] args) {
        try {
            Thread.sleep(3000);
            TcpOrderClient.main(args);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }

    }
}
