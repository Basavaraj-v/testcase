package com.smartwaste;

import com.smartwaste.server.WasteMonitorServer;

/**
 * Main application launcher for the Smart Waste Monitoring System.
 */
public class SmartWasteApplication {
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port parameter, using default: " + DEFAULT_PORT);
            }
        }

        try {
            WasteMonitorServer server = new WasteMonitorServer(port);
            server.start();

            // Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SHUTDOWN] Gracefully stopping Smart Waste Monitoring Server...");
                server.stop();
                System.out.println("[SHUTDOWN] Server stopped.");
            }));

        } catch (Exception e) {
            System.err.println("Failed to start Smart Waste Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
