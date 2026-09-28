package com.smartwaste.service;

import com.smartwaste.model.Bin;
import com.smartwaste.model.RoutePoint;

import java.util.*;

/**
 * Service that implements route optimization algorithms (Nearest-Neighbor TSP with Haversine metrics).
 */
public class RouteOptimizationService {

    // Municipal Waste Operations Depot coordinates (Starting & Ending Hub)
    public static final double DEPOT_LAT = 12.971598;
    public static final double DEPOT_LNG = 77.585000;
    public static final String DEPOT_NAME = "Central Municipal Depot & Garage";

    /**
     * Calculates the great-circle distance between two GPS coordinates using the Haversine formula.
     * @return distance in kilometers
     */
    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Computes the optimal collection route visiting only overflowing/critical bins (fill >= 75%).
     */
    public Map<String, Object> calculateOptimalRoute(List<Bin> allBins) {
        List<Bin> targetBins = new ArrayList<>();
        for (Bin b : allBins) {
            if (b.getFillPercent() >= 75 || "CRITICAL".equalsIgnoreCase(b.getStatus()) || b.isTiltAlert()) {
                targetBins.add(b);
            }
        }

        List<RoutePoint> orderedPoints = new ArrayList<>();
        double currentLat = DEPOT_LAT;
        double currentLng = DEPOT_LNG;
        double totalDistanceKm = 0.0;
        int step = 1;

        // Step 1: Start at Depot
        orderedPoints.add(new RoutePoint(step++, "DEPOT-00", DEPOT_NAME, DEPOT_LAT, DEPOT_LNG, 0, 0.0));

        // Greedy Nearest Neighbor TSP
        Set<String> visited = new HashSet<>();
        while (visited.size() < targetBins.size()) {
            Bin nearestBin = null;
            double shortestDist = Double.MAX_VALUE;

            for (Bin bin : targetBins) {
                if (!visited.contains(bin.getId())) {
                    double dist = calculateHaversineDistance(currentLat, currentLng, bin.getLatitude(), bin.getLongitude());
                    if (dist < shortestDist) {
                        shortestDist = dist;
                        nearestBin = bin;
                    }
                }
            }

            if (nearestBin != null) {
                visited.add(nearestBin.getId());
                totalDistanceKm += shortestDist;
                currentLat = nearestBin.getLatitude();
                currentLng = nearestBin.getLongitude();
                orderedPoints.add(new RoutePoint(step++, nearestBin.getId(), nearestBin.getName(), 
                                                currentLat, currentLng, nearestBin.getFillPercent(), shortestDist));
            } else {
                break;
            }
        }

        // Return to Landfill / Recycling Facility
        double returnDist = calculateHaversineDistance(currentLat, currentLng, DEPOT_LAT, DEPOT_LNG);
        totalDistanceKm += returnDist;
        orderedPoints.add(new RoutePoint(step, "FACILITY-01", "Municipal Solid Waste Processing Plant", 
                                        DEPOT_LAT, DEPOT_LNG, 0, returnDist));

        // Calculate environmental and economic impact
        double unoptimizedDistance = (targetBins.size() + 1) * 3.8; // Baseline legacy fixed route
        double distanceSavedKm = Math.max(0, unoptimizedDistance - totalDistanceKm);
        double fuelSavedLiters = distanceSavedKm * 0.38; // Heavy garbage truck avg 38L/100km
        double co2SavedKg = fuelSavedLiters * 2.68; // 2.68 kg CO2 per liter diesel

        Map<String, Object> result = new HashMap<>();
        result.put("waypoints", orderedPoints);
        result.put("criticalBinsCount", targetBins.size());
        result.put("totalDistanceKm", Math.round(totalDistanceKm * 100.0) / 100.0);
        result.put("distanceSavedKm", Math.round(distanceSavedKm * 100.0) / 100.0);
        result.put("fuelSavedLiters", Math.round(fuelSavedLiters * 10.0) / 10.0);
        result.put("co2SavedKg", Math.round(co2SavedKg * 10.0) / 10.0);
        result.put("estimatedDurationMins", (int)(totalDistanceKm * 3.5 + targetBins.size() * 6)); // Drive time + 6 min stop time

        return result;
    }

    public String routeToJson(Map<String, Object> routeData) {
        @SuppressWarnings("unchecked")
        List<RoutePoint> waypoints = (List<RoutePoint>) routeData.get("waypoints");
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"criticalBinsCount\":").append(routeData.get("criticalBinsCount")).append(",");
        sb.append("\"totalDistanceKm\":").append(routeData.get("totalDistanceKm")).append(",");
        sb.append("\"distanceSavedKm\":").append(routeData.get("distanceSavedKm")).append(",");
        sb.append("\"fuelSavedLiters\":").append(routeData.get("fuelSavedLiters")).append(",");
        sb.append("\"co2SavedKg\":").append(routeData.get("co2SavedKg")).append(",");
        sb.append("\"estimatedDurationMins\":").append(routeData.get("estimatedDurationMins")).append(",");
        sb.append("\"waypoints\":[");
        for (int i = 0; i < waypoints.size(); i++) {
            sb.append(waypoints.get(i).toJson());
            if (i < waypoints.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }
}
