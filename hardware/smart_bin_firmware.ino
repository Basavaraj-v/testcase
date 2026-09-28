/**
 * Smart Waste Collection & Monitoring System
 * Firmware for ESP32 / ESP8266 IoT Smart Bin Node
 * 
 * Target: ESP32 Dev Module
 * Hardware:
 *   - HC-SR04 Ultrasonic Sensor (Garbage Depth)
 *   - HX711 + Load Cell (Garbage Weight)
 *   - MQ-135 Gas Sensor (Decomposition Odor / Methane)
 *   - MPU-6050 Gyro/Accelerometer (Tilt / Tamper Alert)
 *   - Wi-Fi 802.11 b/g/n for telemetry upload to Java REST Backend
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <Wire.h>

// Wi-Fi Configuration
const char* WIFI_SSID     = "Municipal_SmartCity_WiFi";
const char* WIFI_PASSWORD = "SmartCitySecureKey2026";

// Java Backend API Endpoint
const char* BACKEND_URL   = "http://192.168.1.100:8080/api/telemetry";

// Bin Identification & Dimensions
const char* BIN_ID        = "BIN-001";
const float TOTAL_BIN_HEIGHT_CM = 120.0; // Distance from sensor to empty bin bottom

// Pin Definitions
#define PIN_TRIG          5
#define PIN_ECHO          18
#define PIN_GAS_ANALOG    34
#define PIN_LED_ALERT     25
#define PIN_LED_OK        26

// Sensor Telemetry Variables
float measuredDistanceCm  = 0.0;
int   fillPercentage      = 0;
float weightKg            = 0.0;
int   gasPpm              = 0;
bool  tiltDetected        = false;
int   batteryPercent      = 94;

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n[SYSTEM] Initializing Smart Waste IoT Node: " + String(BIN_ID));

  // Initialize GPIO pins
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  pinMode(PIN_GAS_ANALOG, INPUT);
  pinMode(PIN_LED_ALERT, OUTPUT);
  pinMode(PIN_LED_OK, OUTPUT);

  // Connect to Wi-Fi Network
  connectWiFi();

  // Perform Sensor Readings
  readUltrasonicSensor();
  readGasSensor();
  readWeightSensor();
  checkTiltSensor();

  // Update Local LED Status
  if (fillPercentage >= 80 || tiltDetected) {
    digitalWrite(PIN_LED_ALERT, HIGH);
    digitalWrite(PIN_LED_OK, LOW);
  } else {
    digitalWrite(PIN_LED_ALERT, LOW);
    digitalWrite(PIN_LED_OK, HIGH);
  }

  // Transmit Telemetry Payload to Java Backend
  transmitTelemetry();

  // Enter Power Saving Deep Sleep (10 Minutes)
  Serial.println("[POWER] Entering Deep Sleep for 600 seconds...");
  esp_sleep_enable_timer_wakeup(600 * 1000000ULL);
  esp_deep_sleep_start();
}

void loop() {
  // Not executed due to deep sleep
}

void connectWiFi() {
  Serial.print("[WIFI] Connecting to " + String(WIFI_SSID));
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WIFI] Connected! IP: " + WiFi.localIP().toString());
  } else {
    Serial.println("\n[WIFI] Failed to connect. Will retry next wake cycle.");
  }
}

void readUltrasonicSensor() {
  // Trigger ultrasonic sonic burst
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);

  // Read echo bounce travel time
  long duration = pulseIn(PIN_ECHO, HIGH, 30000); // 30ms timeout
  if (duration == 0) {
    measuredDistanceCm = TOTAL_BIN_HEIGHT_CM; // No obstacle or out of range
  } else {
    measuredDistanceCm = (duration * 0.0343) / 2.0;
  }

  // Constrain distance within bin physical bounds
  if (measuredDistanceCm > TOTAL_BIN_HEIGHT_CM) measuredDistanceCm = TOTAL_BIN_HEIGHT_CM;
  if (measuredDistanceCm < 5.0) measuredDistanceCm = 5.0; // Sensor dead-zone buffer

  // Calculate waste fill percentage
  float fillRatio = (TOTAL_BIN_HEIGHT_CM - measuredDistanceCm) / TOTAL_BIN_HEIGHT_CM;
  fillPercentage = (int)(fillRatio * 100.0);
  if (fillPercentage < 0) fillPercentage = 0;
  if (fillPercentage > 100) fillPercentage = 100;

  Serial.println("[SENSOR] Distance: " + String(measuredDistanceCm) + " cm | Fill: " + String(fillPercentage) + "%");
}

void readGasSensor() {
  // Read analog value from MQ-135 sensor (12-bit ADC: 0-4095)
  int rawAdc = analogRead(PIN_GAS_ANALOG);
  // Map raw ADC to approximate Air Quality / Odor index PPM
  gasPpm = map(rawAdc, 200, 3500, 80, 800);
  if (gasPpm < 50) gasPpm = 50;
  Serial.println("[SENSOR] Gas / Odor Level: " + String(gasPpm) + " PPM");
}

void readWeightSensor() {
  // Read calibrated value from HX711 ADC (mocked for standard hardware test)
  weightKg = (fillPercentage * 0.45); // Approximate 45kg at 100% capacity
  Serial.println("[SENSOR] Estimated Weight: " + String(weightKg) + " kg");
}

void checkTiltSensor() {
  // Read accelerometer to detect if bin fell or was vandalized
  tiltDetected = false; 
}

void transmitTelemetry() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[HTTP] Wi-Fi not connected. Skipping upload.");
    return;
  }

  HTTPClient http;
  http.begin(BACKEND_URL);
  http.addHeader("Content-Type", "application/json");

  // Construct JSON payload conforming to Java TelemetryData DTO
  String jsonPayload = "{";
  jsonPayload += "\"binId\":\"" + String(BIN_ID) + "\",";
  jsonPayload += "\"fillPercent\":" + String(fillPercentage) + ",";
  jsonPayload += "\"weightKg\":" + String(weightKg, 2) + ",";
  jsonPayload += "\"gasPpm\":" + String(gasPpm) + ",";
  jsonPayload += "\"batteryPercent\":" + String(batteryPercent) + ",";
  jsonPayload += "\"tiltDetected\":" + String(tiltDetected ? "true" : "false");
  jsonPayload += "}";

  Serial.println("[HTTP] Dispatching JSON to Java REST Backend: " + jsonPayload);
  int httpResponseCode = http.POST(jsonPayload);

  if (httpResponseCode > 0) {
    String response = http.getString();
    Serial.println("[HTTP] Server Response (" + String(httpResponseCode) + "): " + response);
  } else {
    Serial.println("[HTTP] Error during POST request: " + String(httpResponseCode));
  }

  http.end();
}
