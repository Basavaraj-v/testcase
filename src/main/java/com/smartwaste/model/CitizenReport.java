package com.smartwaste.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing a citizen grievance report.
 */
public class CitizenReport {
    private String id;
    private String binId;
    private String reporterName;
    private String contactNumber;
    private double latitude;
    private double longitude;
    private String issueType; // OVERFLOW, FOUL_ODOR, DAMAGED_LID, ILLEGAL_DUMPING
    private String description;
    private String status; // PENDING, DISPATCHED, RESOLVED
    private String createdAt;

    public CitizenReport() {}

    public CitizenReport(String id, String binId, String reporterName, String contactNumber, 
                         double latitude, double longitude, String issueType, String description) {
        this.id = id;
        this.binId = binId;
        this.reporterName = reporterName;
        this.contactNumber = contactNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.issueType = issueType;
        this.description = description;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBinId() { return binId; }
    public void setBinId(String binId) { this.binId = binId; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String toJson() {
        return "{"
            + "\"id\":\"" + escape(id) + "\","
            + "\"binId\":\"" + escape(binId) + "\","
            + "\"reporterName\":\"" + escape(reporterName) + "\","
            + "\"contactNumber\":\"" + escape(contactNumber) + "\","
            + "\"latitude\":" + latitude + ","
            + "\"longitude\":" + longitude + ","
            + "\"issueType\":\"" + escape(issueType) + "\","
            + "\"description\":\"" + escape(description) + "\","
            + "\"status\":\"" + status + "\","
            + "\"createdAt\":\"" + escape(createdAt) + "\""
            + "}";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
