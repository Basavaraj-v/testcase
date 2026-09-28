# 🗑️ IoT Smart Public Waste Collection & Monitoring System (Java Edition)

An enterprise-grade, end-to-end municipal solid waste management and route optimization platform implemented in **Java**. This system monitors urban garbage bins in real-time using IoT telemetry (fill level, weight, toxic gas levels, fire/tamper detection), automatically optimizes collection truck routes using graph algorithms, and provides a citizen grievance reporting portal.

---

## 📌 1. Project Implementation Architecture

The project is structured around a multi-tier IoT & Web architecture:

1. **IoT Sensor & Microcontroller Layer (Field Hardware)**:
   - Ultrasonic sensors (`HC-SR04`) mounted under bin lids measure the empty distance to compute waste fill percentage ($0 - 100\%$).
   - Load cell sensors (`HX711`) measure the physical weight of accumulated refuse in kilograms.
   - Air quality/gas sensors (`MQ-135`) detect volatile organic compounds, ammonia, and methane ($CH_4$) emitted during organic decomposition.
   - Accelerometers (`MPU-6050`) detect if a bin has tilted, fallen, or been vandalized.
   - An **ESP32 microcontroller** processes these sensor readings and transmits an HTTP JSON payload via Wi-Fi/GSM to the Java backend.

2. **Java Backend Layer**:
   - **REST API & Ingestion Engine**: Receives telemetry data from thousands of distributed bins via `POST /api/telemetry`.
   - **Threshold Alert Engine**: Evaluates bin status (`NORMAL`, `WARNING`, `CRITICAL`, `VANDALIZED`) in real time.
   - **Route Optimization Engine (TSP Algorithm)**: Implements graph distance heuristics (Haversine formula + Nearest-Neighbor Travelling Salesperson Problem) to compute the shortest, most fuel-efficient route for collection trucks to empty overflowing bins.
   - **Citizen Grievance Management**: Records public reports of overflowing or damaged bins and dispatches maintenance crews.

3. **Database Layer**:
   - Relational data model storing Bins, Telemetry Logs, Truck Fleets, Collection Logs, and Citizen Grievances.
   - Compatible with **MySQL**, **PostgreSQL**, **H2**, or embedded JSON storage.

4. **Frontend Presentation & Command Center**:
   - Built with **HTML5, CSS3, Modern JavaScript, Leaflet.js, and Chart.js**.
   - Live interactive GIS map with color-coded bin markers (Green = Normal, Orange = Warning, Red = Urgent/Overflowing).
   - Real-time fleet tracking & dynamic path overlay for municipal garbage trucks.
   - Public Citizen Portal for reporting garbage issues with photo upload simulation and geolocation tagging.
   - Built-in IoT Telemetry Hardware Simulator for interactive testing.

---

## 🛠️ 2. Sources & Technologies Required

| Layer | Source / Technology | Purpose |
| :--- | :--- | :--- |
| **Backend Language** | **Java (JDK 17 or JDK 21 / JDK 11+)** | Core backend programming language |
| **Backend Framework** | **Spring Boot / Java HTTP Server** | REST API controllers, routing, services, static asset hosting |
| **Build Tool** | **Maven (`pom.xml`)** | Dependency management & project lifecycle |
| **Database** | **MySQL / PostgreSQL / H2 Database** | Persistent storage for bins, telemetry, fleet, & grievances |
| **Routing Algorithm** | **Java Graph TSP / Haversine Distance** | Dynamic shortest-path collection truck route calculation |
| **Frontend UI** | **HTML5, CSS3, Modern JavaScript (ES6+)** | Responsive municipal dashboard & citizen grievance UI |
| **GIS Mapping** | **Leaflet.js & OpenStreetMap** | Open-source interactive map for bins and truck routes |
| **Analytics** | **Chart.js** | Live visual charts for waste volume and fill rate trends |
| **IoT Firmware** | **C++ / Arduino IDE / ESP-IDF** | Microcontroller firmware for ESP32/ESP8266/Arduino |
| **IoT Sensors** | **HC-SR04, HX711, MQ-135, MPU-6050** | Physical hardware sensors for fill, weight, gas, and tilt |

---

## 📂 3. Project Directory Structure

```
nw one/
├── pom.xml                                    # Maven configuration file (Spring Boot / Java REST)
├── README.md                                  # Complete Java project documentation
├── run_project.bat                            # Windows 1-click launcher (starts Java backend & opens UI)
├── docs/
│   ├── ARCHITECTURE.md                        # In-depth architectural & algorithm breakdown
│   ├── HARDWARE_SPECIFICATION.md              # Circuit pinout, bill of materials, and wiring
│   └── DATABASE_SCHEMA.sql                    # Production SQL schema (MySQL / PostgreSQL / H2)
├── hardware/
│   └── smart_bin_firmware.ino                 # Complete ESP32 C++ firmware
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── smartwaste/
│       │           ├── SmartWasteApplication.java        # Main Application Launcher
│       │           ├── model/
│       │           │   ├── Bin.java                      # Bin domain entity
│       │           │   ├── TelemetryData.java            # Sensor telemetry DTO
│       │           │   ├── CitizenReport.java            # Citizen complaint entity
│       │           │   ├── Truck.java                    # Truck fleet entity
│       │           │   └── RoutePoint.java               # Waypoint model for TSP route
│       │           ├── service/
│       │           │   ├── BinService.java               # In-memory / DB bin management & alerts
│       │           │   ├── RouteOptimizationService.java # Java TSP shortest route algorithm
│       │           │   └── ReportService.java            # Citizen complaint processing
│       │           ├── controller/
│       │           │   ├── BinController.java            # REST endpoints for bin data & telemetry
│       │           │   ├── RouteController.java          # REST endpoints for route optimization
│       │           │   └── ReportController.java         # REST endpoints for citizen complaints
│       │           └── server/
│       │               └── WasteMonitorServer.java       # Pure Java Zero-Dependency HTTP Web Server
│       └── resources/
│           ├── application.properties                    # Configuration properties
│           └── static/                                   # Frontend web application
│               ├── index.html                            # Municipal Dashboard & Citizen Portal
│               ├── styles.css                            # Modern UI styles
│               ├── app.js                                # Frontend logic, Map, & REST client
│               └── simulator.js                          # IoT Hardware Telemetry Simulator
```

---

## 🚀 4. How to Run the Project

### Method 1: Instant 1-Click Launch (Windows)
Double-click `run_project.bat` in the project root directory.
- It will compile and start the Java server on port `8080`.
- It will automatically open the interactive web dashboard in your default browser at `http://localhost:8080`.

### Method 2: Manual Java Run (Zero Third-Party Downloads Needed)
If you have Java installed on your machine (`javac` and `java`):
```bash
# 1. Compile all Java source files
javac -d bin src/main/java/com/smartwaste/*.java src/main/java/com/smartwaste/model/*.java src/main/java/com/smartwaste/service/*.java src/main/java/com/smartwaste/server/*.java

# 2. Run the server
java -cp bin com.smartwaste.SmartWasteApplication
```
Then visit `http://localhost:8080` in your web browser.

### Method 3: Run via Maven
```bash
mvn clean package
mvn spring-boot:run
```

### Method 4: Standalone Browser Mode (No Java Runtime Needed)
If you just want to test or present the UI, interactive map, route optimizer, and IoT simulator immediately:
- Simply open `src/main/resources/static/index.html` directly in any web browser!
- The dashboard automatically detects offline mode and runs with realistic simulated IoT nodes and real-time path calculation.
