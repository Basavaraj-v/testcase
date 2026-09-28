package com.smartwaste.server;

import com.smartwaste.model.Bin;
import com.smartwaste.model.CitizenReport;
import com.smartwaste.model.TelemetryData;
import com.smartwaste.service.BinService;
import com.smartwaste.service.ReportService;
import com.smartwaste.service.RouteOptimizationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in High Performance Java HTTP Web Server and REST API.
 * Uses standard Java SE runtime with zero external Maven library requirements.
 */
public class WasteMonitorServer {
    private final int port;
    private final BinService binService;
    private final RouteOptimizationService routeService;
    private final ReportService reportService;
    private HttpServer server;

    public WasteMonitorServer(int port) {
        this.port = port;
        this.binService = new BinService();
        this.routeService = new RouteOptimizationService();
        this.reportService = new ReportService();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        // REST API Handlers
        server.createContext("/api/bins", new BinsHandler());
        server.createContext("/api/telemetry", new TelemetryHandler());
        server.createContext("/api/bins/empty", new EmptyBinHandler());
        server.createContext("/api/routes/optimize", new RouteOptimizationHandler());
        server.createContext("/api/reports", new ReportsHandler());
        server.createContext("/api/stats", new StatsHandler());

        // Static Web Asset Handler (Dashboard UI)
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("=================================================================");
        System.out.println(" 🗑️  Smart Waste Collection & Monitoring Server (Java)");
        System.out.println(" 🌐  Web Dashboard: http://localhost:" + port);
        System.out.println(" 📡  IoT Ingestion: http://localhost:" + port + "/api/telemetry");
        System.out.println(" 🚀  Route Optimizer: http://localhost:" + port + "/api/routes/optimize");
        System.out.println("=================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ==========================================
    // REST API HANDLERS
    // ==========================================

    private class BinsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String json = binService.getBinsJson();
                sendJsonResponse(exchange, 200, json);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class TelemetryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                TelemetryData telemetry = parseTelemetryJson(body);
                if (telemetry != null && telemetry.getBinId() != null) {
                    binService.updateTelemetry(telemetry);
                    sendJsonResponse(exchange, 200, "{\"status\":\"SUCCESS\",\"message\":\"Telemetry ingested\",\"binId\":\"" + telemetry.getBinId() + "\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"status\":\"ERROR\",\"message\":\"Invalid telemetry payload\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class EmptyBinHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                String binId = extractJsonString(body, "binId");
                if (binId != null && binService.emptyBin(binId)) {
                    sendJsonResponse(exchange, 200, "{\"status\":\"SUCCESS\",\"message\":\"Bin emptied successfully\",\"binId\":\"" + binId + "\"}");
                } else {
                    sendJsonResponse(exchange, 404, "{\"status\":\"ERROR\",\"message\":\"Bin not found\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class RouteOptimizationHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                List<Bin> bins = binService.getAllBins();
                Map<String, Object> route = routeService.calculateOptimalRoute(bins);
                String json = routeService.routeToJson(route);
                sendJsonResponse(exchange, 200, json);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class ReportsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 200, reportService.getReportsJson());
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                String name = extractJsonString(body, "reporterName");
                String contact = extractJsonString(body, "contactNumber");
                String issue = extractJsonString(body, "issueType");
                String desc = extractJsonString(body, "description");
                String binId = extractJsonString(body, "binId");
                double lat = extractJsonDouble(body, "latitude", 12.9716);
                double lng = extractJsonDouble(body, "longitude", 77.5946);

                CitizenReport report = new CitizenReport(null, binId, name, contact, lat, lng, issue, desc);
                reportService.submitReport(report);
                sendJsonResponse(exchange, 201, report.toJson());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            List<Bin> bins = binService.getAllBins();
            int totalBins = bins.size();
            long criticalCount = bins.stream().filter(b -> "CRITICAL".equalsIgnoreCase(b.getStatus()) || b.isTiltAlert()).count();
            long warningCount = bins.stream().filter(b -> "WARNING".equalsIgnoreCase(b.getStatus())).count();
            long normalCount = bins.stream().filter(b -> "NORMAL".equalsIgnoreCase(b.getStatus())).count();
            double totalWeightKg = bins.stream().mapToDouble(Bin::getWeightKg).sum();

            String json = "{"
                + "\"totalBins\":" + totalBins + ","
                + "\"criticalCount\":" + criticalCount + ","
                + "\"warningCount\":" + warningCount + ","
                + "\"normalCount\":" + normalCount + ","
                + "\"totalWeightKg\":" + Math.round(totalWeightKg * 10.0) / 10.0 + ","
                + "\"totalReports\":" + reportService.getAllReports().size()
                + "}";
            sendJsonResponse(exchange, 200, json);
        }
    }

    // ==========================================
    // STATIC FILE HANDLER (WEB DASHBOARD)
    // ==========================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Look for resource in file system or classpath
            File file = resolveStaticFile(path);
            if (file != null && file.exists() && !file.isDirectory()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Attempt to read from classpath
                InputStream is = getClass().getResourceAsStream("/static" + path);
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    exchange.getResponseHeaders().set("Content-Type", getContentType(path));
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                } else {
                    String notFound = "<h1>404 Not Found</h1><p>Static asset " + path + " not found.</p>";
                    byte[] bytes = notFound.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(404, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(bytes);
                    }
                }
            }
        }

        private File resolveStaticFile(String relativePath) {
            // Check frontend/ or src/main/resources/static/
            String[] searchPaths = {
                "src/main/resources/static" + relativePath,
                "frontend" + relativePath,
                "." + relativePath
            };
            for (String p : searchPaths) {
                File f = new File(p);
                if (f.exists()) return f;
            }
            return null;
        }

        private String getContentType(String path) {
            if (path.endsWith(".html")) return "text/html; charset=UTF-8";
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".json")) return "application/json; charset=UTF-8";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".svg")) return "image/svg+xml";
            return "text/plain; charset=UTF-8";
        }
    }

    // ==========================================
    // UTILITIES & PARSERS
    // ==========================================

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private TelemetryData parseTelemetryJson(String json) {
        if (json == null || json.isEmpty()) return null;
        String binId = extractJsonString(json, "binId");
        int fill = extractJsonInt(json, "fillPercent", 0);
        double weight = extractJsonDouble(json, "weightKg", 0.0);
        int gas = extractJsonInt(json, "gasPpm", 100);
        int battery = extractJsonInt(json, "batteryPercent", 90);
        boolean tilt = json.contains("\"tiltDetected\":true") || json.contains("\"tiltAlert\":true");
        return new TelemetryData(binId, fill, weight, gas, battery, tilt);
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    private int extractJsonInt(String json, String key, int defaultVal) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private double extractJsonDouble(String json, String key, double defaultVal) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }
}
