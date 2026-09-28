package com.smartwaste.model;

/**
 * Represents a navigational waypoint in an optimized collection route.
 */
public class RoutePoint {
    private int step;
    private String binId;
    private String name;
    private double latitude;
    private double longitude;
    private int fillPercent;
    private double distanceKmFromPrev;

    public RoutePoint(int step, String binId, String name, double latitude, double longitude, int fillPercent, double distanceKmFromPrev) {
        this.step = step;
        this.binId = binId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.fillPercent = fillPercent;
        this.distanceKmFromPrev = distanceKmFromPrev;
    }

    public int getStep() { return step; }
    public String getBinId() { return binId; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getFillPercent() { return fillPercent; }
    public double getDistanceKmFromPrev() { return distanceKmFromPrev; }

    public String toJson() {
        return "{"
            + "\"step\":" + step + ","
            + "\"binId\":\"" + escape(binId) + "\","
            + "\"name\":\"" + escape(name) + "\","
            + "\"latitude\":" + latitude + ","
            + "\"longitude\":" + longitude + ","
            + "\"fillPercent\":" + fillPercent + ","
            + "\"distanceKmFromPrev\":" + String.format(java.util.Locale.US, "%.2f", distanceKmFromPrev)
            + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
