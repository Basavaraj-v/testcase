package com.smartwaste.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing an IoT-monitored Public Waste Bin.
 */
public class Bin {
    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private String zone;
    private String wasteType; // ORGANIC, RECYCLABLE, GENERAL, HAZARDOUS
    private int capacityLiters;
    private int fillPercent;
    private double weightKg;
    private int gasPpm;
    private int batteryPercent;
    private boolean tiltAlert;
    private String status; // NORMAL, WARNING, CRITICAL, VANDALIZED, OFFLINE
    private String lastUpdated;

    public Bin() {}

    public Bin(String id, String name, double latitude, double longitude, String zone, 
               String wasteType, int fillPercent, double weightKg, int gasPpm) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.zone = zone;
        this.wasteType = wasteType;
        this.capacityLiters = 240;
        this.fillPercent = fillPercent;
        this.weightKg = weightKg;
        this.gasPpm = gasPpm;
        this.batteryPercent = 95;
        this.tiltAlert = false;
        this.lastUpdated = LocalDateTime.now().toString();
        updateStatus();
    }

    public void updateStatus() {
        if (tiltAlert) {
            this.status = "VANDALIZED";
        } else if (fillPercent >= 80 || gasPpm >= 450) {
            this.status = "CRITICAL";
        } else if (fillPercent >= 60 || gasPpm >= 250) {
            this.status = "WARNING";
        } else {
            this.status = "NORMAL";
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public int getCapacityLiters() { return capacityLiters; }
    public void setCapacityLiters(int capacityLiters) { this.capacityLiters = capacityLiters; }

    public int getFillPercent() { return fillPercent; }
    public void setFillPercent(int fillPercent) { 
        this.fillPercent = fillPercent; 
        updateStatus();
    }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public int getGasPpm() { return gasPpm; }
    public void setGasPpm(int gasPpm) { 
        this.gasPpm = gasPpm; 
        updateStatus();
    }

    public int getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(int batteryPercent) { this.batteryPercent = batteryPercent; }

    public boolean isTiltAlert() { return tiltAlert; }
    public void setTiltAlert(boolean tiltAlert) { 
        this.tiltAlert = tiltAlert; 
        updateStatus();
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }

    public String toJson() {
        return "{"
            + "\"id\":\"" + escape(id) + "\","
            + "\"name\":\"" + escape(name) + "\","
            + "\"latitude\":" + latitude + ","
            + "\"longitude\":" + longitude + ","
            + "\"zone\":\"" + escape(zone) + "\","
            + "\"wasteType\":\"" + escape(wasteType) + "\","
            + "\"capacityLiters\":" + capacityLiters + ","
            + "\"fillPercent\":" + fillPercent + ","
            + "\"weightKg\":" + weightKg + ","
            + "\"gasPpm\":" + gasPpm + ","
            + "\"batteryPercent\":" + batteryPercent + ","
            + "\"tiltAlert\":" + tiltAlert + ","
            + "\"status\":\"" + status + "\","
            + "\"lastUpdated\":\"" + escape(lastUpdated) + "\""
            + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
