package com.smartwaste.model;

/**
 * Domain entity representing a municipal waste collection truck.
 */
public class Truck {
    private String id;
    private String driverName;
    private String licensePlate;
    private double capacityKg;
    private double currentLoadKg;
    private double latitude;
    private double longitude;
    private String status; // IDLE, EN_ROUTE, COLLECTING, MAINTENANCE

    public Truck() {}

    public Truck(String id, String driverName, String licensePlate, double capacityKg, 
                 double currentLoadKg, double latitude, double longitude, String status) {
        this.id = id;
        this.driverName = driverName;
        this.licensePlate = licensePlate;
        this.capacityKg = capacityKg;
        this.currentLoadKg = currentLoadKg;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public double getCapacityKg() { return capacityKg; }
    public void setCapacityKg(double capacityKg) { this.capacityKg = capacityKg; }

    public double getCurrentLoadKg() { return currentLoadKg; }
    public void setCurrentLoadKg(double currentLoadKg) { this.currentLoadKg = currentLoadKg; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String toJson() {
        return "{"
            + "\"id\":\"" + escape(id) + "\","
            + "\"driverName\":\"" + escape(driverName) + "\","
            + "\"licensePlate\":\"" + escape(licensePlate) + "\","
            + "\"capacityKg\":" + capacityKg + ","
            + "\"currentLoadKg\":" + currentLoadKg + ","
            + "\"latitude\":" + latitude + ","
            + "\"longitude\":" + longitude + ","
            + "\"status\":\"" + status + "\""
            + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
