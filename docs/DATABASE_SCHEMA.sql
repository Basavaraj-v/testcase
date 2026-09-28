-- ==============================================================
-- IoT Smart Public Waste Collection & Monitoring System
-- Database Schema Definition (Compatible with MySQL, PostgreSQL, H2)
-- ==============================================================

-- 1. Municipal Waste Bins Table
CREATE TABLE IF NOT EXISTS bins (
    bin_id VARCHAR(20) PRIMARY KEY,
    bin_name VARCHAR(100) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    area_zone VARCHAR(50) NOT NULL,
    waste_type VARCHAR(30) DEFAULT 'GENERAL', -- GENERAL, ORGANIC, RECYCLABLE, HAZARDOUS
    capacity_liters INT DEFAULT 240,
    total_height_cm INT DEFAULT 120,
    current_fill_percent INT DEFAULT 0,
    current_weight_kg DECIMAL(6, 2) DEFAULT 0.0,
    gas_ppm INT DEFAULT 100,
    battery_percent INT DEFAULT 100,
    tilt_status BOOLEAN DEFAULT FALSE,
    bin_status VARCHAR(20) DEFAULT 'NORMAL', -- NORMAL, WARNING, CRITICAL, VANDALIZED, OFFLINE
    last_telemetry_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Real-time Telemetry Historical Logs Table
CREATE TABLE IF NOT EXISTS telemetry_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bin_id VARCHAR(20) NOT NULL,
    fill_percent INT NOT NULL,
    weight_kg DECIMAL(6, 2) NOT NULL,
    gas_ppm INT NOT NULL,
    tilt_detected BOOLEAN DEFAULT FALSE,
    battery_level INT DEFAULT 100,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bin_telemetry FOREIGN KEY (bin_id) REFERENCES bins(bin_id) ON DELETE CASCADE
);

-- 3. Waste Collection Trucks & Fleet Table
CREATE TABLE IF NOT EXISTS collection_trucks (
    truck_id VARCHAR(20) PRIMARY KEY,
    driver_name VARCHAR(100) NOT NULL,
    license_plate VARCHAR(30) NOT NULL UNIQUE,
    capacity_kg DECIMAL(8, 2) DEFAULT 5000.0,
    current_load_kg DECIMAL(8, 2) DEFAULT 0.0,
    current_latitude DECIMAL(10, 7),
    current_longitude DECIMAL(10, 7),
    status VARCHAR(20) DEFAULT 'IDLE', -- IDLE, EN_ROUTE, COLLECTING, MAINTENANCE
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Dispatch Routes & Waypoints Table
CREATE TABLE IF NOT EXISTS collection_routes (
    route_id VARCHAR(30) PRIMARY KEY,
    truck_id VARCHAR(20) NOT NULL,
    start_depot_lat DECIMAL(10, 7) NOT NULL,
    start_depot_lng DECIMAL(10, 7) NOT NULL,
    total_distance_km DECIMAL(6, 2) DEFAULT 0.0,
    estimated_duration_mins INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ASSIGNED', -- ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT fk_route_truck FOREIGN KEY (truck_id) REFERENCES collection_trucks(truck_id)
);

-- 5. Route Waypoint Steps (Sequence of Bins to Visit)
CREATE TABLE IF NOT EXISTS route_waypoints (
    waypoint_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id VARCHAR(30) NOT NULL,
    bin_id VARCHAR(20) NOT NULL,
    stop_sequence INT NOT NULL,
    is_collected BOOLEAN DEFAULT FALSE,
    collected_at TIMESTAMP NULL,
    CONSTRAINT fk_waypoint_route FOREIGN KEY (route_id) REFERENCES collection_routes(route_id) ON DELETE CASCADE,
    CONSTRAINT fk_waypoint_bin FOREIGN KEY (bin_id) REFERENCES bins(bin_id)
);

-- 6. Citizen Grievance & Public Issue Reports Table
CREATE TABLE IF NOT EXISTS citizen_reports (
    report_id VARCHAR(30) PRIMARY KEY,
    bin_id VARCHAR(20),
    reporter_name VARCHAR(100),
    reporter_contact VARCHAR(50),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(10, 7) NOT NULL,
    issue_type VARCHAR(40) NOT NULL, -- OVERFLOW, BAD_ODOR, DAMAGED_LID, ILLEGAL_DUMPING, FIRE_SMOKE
    description TEXT,
    photo_url VARCHAR(255),
    report_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, INVESTIGATING, DISPATCHED, RESOLVED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL
);

-- ==============================================================
-- Initial Mock Data Seeds for Demonstration
-- ==============================================================

INSERT INTO bins (bin_id, bin_name, latitude, longitude, area_zone, waste_type, current_fill_percent, current_weight_kg, gas_ppm, battery_percent, bin_status) VALUES
('BIN-001', 'City Hall Plaza Receptacle', 12.971598, 77.594562, 'Zone-Central', 'ORGANIC', 88, 38.4, 420, 92, 'CRITICAL'),
('BIN-002', 'Metro Station Gate 2', 12.978310, 77.599600, 'Zone-Central', 'RECYCLABLE', 45, 14.2, 110, 85, 'NORMAL'),
('BIN-003', 'Public Market Food Court', 12.965400, 77.587800, 'Zone-South', 'ORGANIC', 94, 47.0, 580, 78, 'CRITICAL'),
('BIN-004', 'Commercial Boulevard 4th Cross', 12.982000, 77.605000, 'Zone-East', 'GENERAL', 76, 29.5, 230, 90, 'WARNING'),
('BIN-005', 'Community Hospital Parking', 12.960200, 77.601200, 'Zone-South', 'HAZARDOUS', 30, 9.8, 90, 95, 'NORMAL'),
('BIN-006', 'Central Park Jogging Track', 12.975000, 77.591000, 'Zone-Central', 'RECYCLABLE', 82, 33.1, 160, 88, 'CRITICAL'),
('BIN-007', 'University Library Entrance', 12.986000, 77.589000, 'Zone-North', 'GENERAL', 22, 6.5, 80, 99, 'NORMAL');

INSERT INTO collection_trucks (truck_id, driver_name, license_plate, capacity_kg, current_load_kg, current_latitude, current_longitude, status) VALUES
('TRK-01', 'Rajesh Sharma', 'KA-01-EA-4821', 6000.0, 1250.0, 12.970000, 77.590000, 'EN_ROUTE'),
('TRK-02', 'Vikram Singh', 'KA-04-MB-9012', 5000.0, 0.0, 12.985000, 77.608000, 'IDLE');
