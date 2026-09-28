# 🏗️ System Architecture & Implementation

## 1. System Architecture Overview

The **Public Waste Collection Monitoring System** connects distributed physical waste receptacles across urban sectors to a central municipal management cloud platform.

```
+-------------------------------------------------------------------------------+
|                             FIELD HARDWARE LAYER                              |
|                                                                               |
|  [Ultrasonic HC-SR04]  [HX711 Load Cell]  [MQ-135 Gas]  [MPU6050 Accelerometer]|
|            \                 |                  |                 /           |
|             \                |                  |                /            |
|              +---------------+------------------+---------------+             |
|                                      |                                        |
|                          [ESP32 / NodeMCU Microcontroller]                    |
|                                      |                                        |
|                     [Wi-Fi 802.11 / 4G LTE / LoRaWAN]                         |
+--------------------------------------|----------------------------------------+
                                       | HTTPS POST / MQTT
                                       v
+-------------------------------------------------------------------------------+
|                             BACKEND CLOUD LAYER                               |
|                                                                               |
|                     [Node.js / Express.js REST API Server]                    |
|                        ├── /api/telemetry (IoT Ingestion)                     |
|                        ├── /api/bins      (Bin Registry)                      |
|                        ├── /api/routes    (TSP Dynamic Routing)               |
|                        ├── /api/reports   (Citizen Grievances)                |
|                        └── /api/analytics (Volume Trends)                     |
|                                      |                                        |
|                         [Persistent Storage / DB Layer]                       |
|                          ├── bins (GPS, type, status, thresholds)             |
|                          ├── telemetry_logs (fill, weight, gas, timestamp)    |
|                          ├── citizen_reports (coordinates, photos, status)    |
|                          └── trucks (fleet capacity, driver, active route)    |
+--------------------------------------|----------------------------------------+
                                       | JSON REST / SSE
                                       v
+-------------------------------------------------------------------------------+
|                            FRONTEND PRESENTATION LAYER                        |
|                                                                               |
|   [Municipal Command Dashboard]                 [Citizen Reporting Portal]    |
|   ├── Leaflet.js Interactive Map                ├── Geo-tagged Bin Grievance  |
|   ├── Fleet Route Visualization                 ├── Overflow Photo Capture    |
|   ├── Sensor Alert Toast Monitor                └── Ticket Status Tracking    |
|   └── Chart.js Fill Statistics                                                |
+-------------------------------------------------------------------------------+
```

---

## 2. IoT Data Flow & Telemetry Ingestion

1. **Cycle Trigger**:
   - The ESP32 wakes from Deep Sleep every 5 to 15 minutes (or immediately on tilt/tamper interrupt from the MPU6050 accelerometer).
2. **Measurement**:
   - The ultrasonic sensor fires a $40\text{ kHz}$ sonic pulse down the bin container.
   - Fill percentage is calculated as:
     $$\text{Fill Percentage} = \frac{\text{Total Bin Height} - \text{Measured Distance}}{\text{Total Bin Height}} \times 100\%$$
   - The load cell reads the mass of waste deposited.
   - The MQ-135 sensor reads VOC / methane levels in PPM.
3. **Transmission**:
   - Payload is formatted in JSON and securely pushed to `POST /api/telemetry`.
4. **Backend Processing**:
   - If `fillLevel >= 80%`, the bin state switches to `CRITICAL` and triggers an urgent collection flag.
   - If `gasLevel >= 400 PPM`, an alert for organic rot/fire hazard is recorded.
   - If `tiltAlert === true`, an alert for vandalism/toppled bin is triggered.

---

## 3. Dynamic Route Optimization Algorithm (TSP Heuristic)

Traditional trucks follow static routes every day, regardless of bin contents. EcoRoute uses dynamic route calculation:

1. **Filter**: Identify the subset of bins where $\text{Fill Level} \ge 80\%$ or an urgent citizen report is validated.
2. **Matrix Construction**: Compute the distance matrix between the Municipal Depot and all critical bins using the Haversine distance formula:
   $$d = 2r \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
3. **Nearest-Neighbor TSP**:
   - Start at the Depot $[lat_0, lng_0]$.
   - Visit the nearest unvisited overflowing bin.
   - Repeat until all target bins are visited.
   - End at the Municipal Waste Processing Facility / Landfill.
4. **Fleet Impact**: Reduces municipal fuel expenditure and carbon footprint by an estimated $35\text{--}48\%$.

---

## 4. Database Schema (Entities & Relationships)

### Bins Table
- `id` (VARCHAR, PK): e.g. `BIN-101`
- `name` (VARCHAR): e.g. "Central Market - North Wing"
- `latitude` (FLOAT): e.g. `12.9716`
- `longitude` (FLOAT): e.g. `77.5946`
- `waste_type` (ENUM): `Organic`, `Recyclable`, `Hazardous`, `General`
- `total_height_cm` (INT): Default `120`
- `current_fill` (INT): `0 - 100` (%)
- `current_weight` (FLOAT): Weight in kg
- `gas_ppm` (INT): Odor/methane measurement
- `battery_level` (INT): Battery percentage
- `status` (ENUM): `NORMAL`, `WARNING`, `CRITICAL`, `OFFLINE`
- `last_updated` (TIMESTAMP)

### Telemetry Logs Table
- `log_id` (INT, PK AUTO_INCREMENT)
- `bin_id` (VARCHAR, FK -> bins.id)
- `fill_percentage` (INT)
- `weight_kg` (FLOAT)
- `gas_ppm` (INT)
- `tilt_detected` (BOOLEAN)
- `recorded_at` (TIMESTAMP)

### Citizen Reports Table
- `report_id` (VARCHAR, PK): e.g. `REP-8021`
- `bin_id` (VARCHAR, Nullable)
- `citizen_name` (VARCHAR)
- `citizen_phone` (VARCHAR)
- `latitude` (FLOAT)
- `longitude` (FLOAT)
- `issue_type` (ENUM): `OVERFLOWING`, `DAMAGED_LID`, `FOUL_ODOR`, `ILLEGAL_DUMPING`
- `photo_url` (VARCHAR)
- `status` (ENUM): `PENDING`, `DISPATCHED`, `RESOLVED`
- `created_at` (TIMESTAMP)

### Trucks Table
- `truck_id` (VARCHAR, PK): e.g. `TRK-01`
- `driver_name` (VARCHAR)
- `license_plate` (VARCHAR)
- `capacity_tons` (FLOAT)
- `current_load_tons` (FLOAT)
- `current_lat` (FLOAT)
- `current_lng` (FLOAT)
- `status` (ENUM): `IDLE`, `EN_ROUTE`, `COLLECTING`, `MAINTENANCE`
