# 🔌 Hardware Specifications & Circuit Design

## 1. Bill of Materials (BOM)

| Item | Component | Model / Spec | Est. Unit Cost | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| 1 | Microcontroller | ESP-WROOM-32 (38 Pins) | $4.50 | Brain of the node, Wi-Fi/BLE enabled |
| 2 | Ultrasonic Range Sensor | HC-SR04 or JSN-SR04T (Waterproof) | $3.20 | Garbage level distance measurement |
| 3 | Load Cell & ADC | 50kg Bar Load Cell + HX711 24-bit ADC | $4.00 | Waste weight measurement |
| 4 | Gas / Air Quality Sensor | MQ-135 or MQ-4 | $2.50 | Harmful volatile gases & methane detection |
| 5 | Accelerometer / Gyro | MPU-6050 (I2C) | $1.80 | Detects if bin is knocked over or vandalized |
| 6 | Power Supply | 18650 Li-ion 3.7V 3000mAh Battery + TP4056 | $3.50 | Independent rechargeable power |
| 7 | Solar Panel | 5V 1W Monocrystalline Panel | $2.80 | Continuous outdoor energy harvesting |
| 8 | Status Indicator | RGB Common Cathode LED + 220Ω Resistors | $0.30 | Local visual fill-level status |
| 9 | Enclosure | IP65 Weatherproof ABS Junction Box | $3.00 | Protects electronics from rain & waste moisture |
| **Total** | | | **~$25.60** | Per smart bin unit |

---

## 2. Pin Connection Diagram (ESP32)

```
        +-----------------------------------+
        |            ESP-WROOM-32           |
        |                                   |
        | [3V3 / 5V] -------------------+---|---> Power Rails
        | [GND]      -------------------+---|---> Common Ground
        |                                   |
        | [GPIO 5]   -----> HC-SR04 TRIG    |
        | [GPIO 18]  <----- HC-SR04 ECHO    |
        |                                   |
        | [GPIO 19]  <----- HX711 DOUT      |
        | [GPIO 21]  -----> HX711 SCK       |
        |                                   |
        | [GPIO 34]  <----- MQ-135 AOUT     | (ADC1 Analog Input)
        |                                   |
        | [GPIO 22]  <----> MPU6050 SCL     | (I2C Clock)
        | [GPIO 23]  <----> MPU6050 SDA     | (I2C Data)
        |                                   |
        | [GPIO 25]  -----> Red LED (Full)  |
        | [GPIO 26]  -----> Green LED (OK)  |
        +-----------------------------------+
```

---

## 3. Power Optimization Strategy (Deep Sleep)
Waste fill levels change gradually over hours. To allow months of operation on a single battery charge:
1. The ESP32 stays in **Deep Sleep** consuming less than $10\mu\text{A}$.
2. A timer interrupt wakes the ESP32 every 10 minutes.
3. Sensors are powered on via a MOSFET gate switch for only 1.5 seconds.
4. Measurements are taken and averaged over 5 samples to prevent ultrasonic false echoes.
5. Wi-Fi connects, transmits HTTP JSON POST payload, and ESP32 returns to sleep immediately.
6. The MPU6050 interrupt line can wake the ESP32 instantly if an unexpected physical impact occurs.
