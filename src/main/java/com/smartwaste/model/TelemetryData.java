package com.smartwaste.model;

/**
 * Data Transfer Object (DTO) for IoT sensor telemetry packets.
 */
public class TelemetryData {
    private String binId;
    private int fillPercent;
    private double weightKg;
    private int gasPpm;
    private int batteryPercent;
    private boolean tiltDetected;

    public TelemetryData() {}

    public TelemetryData(String binId, int fillPercent, double weightKg, int gasPpm, int batteryPercent, boolean tiltDetected) {
        this.binId = binId;
        this.fillPercent = fillPercent;
        this.weightKg = weightKg;
        this.gasPpm = gasPpm;
        this.batteryPercent = batteryPercent;
        this.tiltDetected = tiltDetected;
    }

    public String getBinId() { return binId; }
    public void setBinId(String binId) { this.binId = binId; }

    public int getFillPercent() { return fillPercent; }
    public void setFillPercent(int fillPercent) { this.fillPercent = fillPercent; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public int getGasPpm() { return gasPpm; }
    public void setGasPpm(int gasPpm) { this.gasPpm = gasPpm; }

    public int getBatteryPercent() { return batteryPercent; }
    public void setBatteryPercent(int batteryPercent) { this.batteryPercent = batteryPercent; }

    public boolean isTiltDetected() { return tiltDetected; }
    public void setTiltDetected(boolean tiltDetected) { this.tiltDetected = tiltDetected; }
}
