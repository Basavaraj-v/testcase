package com.smartwaste.service;

import com.smartwaste.model.CitizenReport;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service managing citizen grievance reports.
 */
public class ReportService {
    private final Map<String, CitizenReport> reports = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1001);

    public ReportService() {
        seedReports();
    }

    private void seedReports() {
        submitReport(new CitizenReport("REP-1001", "BIN-001", "Ananya Rao", "+91 98765 43210", 
                                      12.971598, 77.594562, "OVERFLOW", "Bin is spilling food waste onto pedestrian footpath."));
        submitReport(new CitizenReport("REP-1002", "BIN-003", "Karthik Verma", "+91 98111 22334", 
                                      12.965400, 77.587800, "FOUL_ODOR", "Severe decomposition smell near market entrance."));
    }

    public List<CitizenReport> getAllReports() {
        return new ArrayList<>(reports.values());
    }

    public CitizenReport submitReport(CitizenReport report) {
        if (report.getId() == null || report.getId().isEmpty()) {
            report.setId("REP-" + idCounter.getAndIncrement());
        }
        reports.put(report.getId(), report);
        return report;
    }

    public boolean updateStatus(String reportId, String newStatus) {
        CitizenReport report = reports.get(reportId);
        if (report != null) {
            report.setStatus(newStatus);
            return true;
        }
        return false;
    }

    public String getReportsJson() {
        StringBuilder sb = new StringBuilder("[");
        List<CitizenReport> list = getAllReports();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
