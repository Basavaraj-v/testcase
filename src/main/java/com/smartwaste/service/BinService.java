package com.smartwaste.service;

import com.smartwaste.model.Bin;
import com.smartwaste.model.TelemetryData;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing smart bin records, telemetry updates, and alert state.
 */
public class BinService {
    private final Map<String, Bin> binRepository = new ConcurrentHashMap<>();

    public BinService() {
        seedInitialBins();
    }

    private void seedInitialBins() {
        addOrUpdate(new Bin("BIN-001", "City Hall Main Plaza", 12.971598, 77.594562, "Zone-Central", "ORGANIC", 88, 38.4, 420));
        addOrUpdate(new Bin("BIN-002", "Metro Station Gate 2", 12.978310, 77.599600, "Zone-Central", "RECYCLABLE", 45, 14.2, 110));
        addOrUpdate(new Bin("BIN-003", "Public Market Food Court", 12.965400, 77.587800, "Zone-South", "ORGANIC", 94, 47.0, 580));
        addOrUpdate(new Bin("BIN-004", "Commercial Blvd 4th Cross", 12.982000, 77.605000, "Zone-East", "GENERAL", 76, 29.5, 230));
        addOrUpdate(new Bin("BIN-005", "Community Hospital Grounds", 12.960200, 77.601200, "Zone-South", "HAZARDOUS", 30, 9.8, 90));
        addOrUpdate(new Bin("BIN-006", "Central Park Jogging Path", 12.975000, 77.591000, "Zone-Central", "RECYCLABLE", 82, 33.1, 160));
        addOrUpdate(new Bin("BIN-007", "Tech Park Main Gate", 12.986000, 77.589000, "Zone-North", "GENERAL", 22, 6.5, 80));
        addOrUpdate(new Bin("BIN-008", "Railway Station Bus Terminal", 12.977500, 77.572000, "Zone-West", "GENERAL", 91, 44.2, 390));
    }

    public List<Bin> getAllBins() {
        return new ArrayList<>(binRepository.values());
    }

    public Optional<Bin> getBinById(String id) {
        return Optional.ofNullable(binRepository.get(id));
    }

    public void addOrUpdate(Bin bin) {
        binRepository.put(bin.getId(), bin);
    }

    public boolean updateTelemetry(TelemetryData telemetry) {
        Bin bin = binRepository.get(telemetry.getBinId());
        if (bin == null) {
            // Register newly discovered IoT node automatically
            bin = new Bin(telemetry.getBinId(), "Smart Bin " + telemetry.getBinId(), 
                         12.97 + (Math.random() * 0.02 - 0.01), 
                         77.59 + (Math.random() * 0.02 - 0.01), 
                         "Zone-Central", "GENERAL", 0, 0, 0);
            binRepository.put(telemetry.getBinId(), bin);
        }

        bin.setFillPercent(telemetry.getFillPercent());
        bin.setWeightKg(telemetry.getWeightKg());
        bin.setGasPpm(telemetry.getGasPpm());
        bin.setBatteryPercent(telemetry.getBatteryPercent());
        bin.setTiltAlert(telemetry.isTiltDetected());
        bin.setLastUpdated(LocalDateTime.now().toString());
        return true;
    }

    public boolean emptyBin(String id) {
        Bin bin = binRepository.get(id);
        if (bin != null) {
            bin.setFillPercent(0);
            bin.setWeightKg(1.2); // Tare weight of empty bin liner
            bin.setGasPpm(65);
            bin.setTiltAlert(false);
            bin.setLastUpdated(LocalDateTime.now().toString());
            return true;
        }
        return false;
    }

    public String getBinsJson() {
        StringBuilder sb = new StringBuilder("[");
        List<Bin> bins = getAllBins();
        for (int i = 0; i < bins.size(); i++) {
            sb.append(bins.get(i).toJson());
            if (i < bins.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
