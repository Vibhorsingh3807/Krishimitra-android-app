# 🌾 KrishiMitra — Field Digital Twin Specification

---

## 1. What is the Field Digital Twin?

The **Field Digital Twin** in KrishiMitra is a living, multi-temporal digital representation of a farmer's physical field. Rather than functioning as a static GIS map or simple analytics dashboard, the Digital Twin mirrors the ongoing biological, environmental, and agronomic reality of the field partitioned into manageable management zones.

The system closes the physical-to-digital agricultural feedback loop:
$$\text{REAL FIELD} \longrightarrow \text{DIGITAL TWIN} \longrightarrow \text{ANALYSIS} \longrightarrow \text{RECOMMENDATION} \longrightarrow \text{FARMER ACTION} \longrightarrow \text{UPDATED TWIN}$$

---

## 2. Architecture & System Design

```
                 PHYSICAL REAL FIELD (e.g. 5.2 Acres)
                               │
                               ▼
               FIELD BOUNDARY (Geospatial Polygon)
                               │
                               ▼
             FIELD ZONING ENGINE (3x3 Grid -> 9 Zones)
                               │
             ┌─────────────────┼─────────────────┐
             ▼                 ▼                 ▼
     WEATHER SYSTEM   OBSERVATION PROVIDER  FARMER ACTIONS
     • Temperature     • NDVI (Vegetation)   • Irrigation (35mm)
     • Rainfall & Prob • Soil Moisture %     • Leaf Disease Scans
     • Canopy Humidity • Health Status       • Visual Inspections
             │                 │                 │
             └─────────────────┼─────────────────┘
                               ▼
                      ZONE DIGITAL STATE
             (Time-Series Observations + Risk Index)
                               │
                               ▼
                      FIELD DIGITAL TWIN
         🟢 Healthy (Z1-4, Z6) | 🟡 Moderate (Z5, Z8-9) | 🔴 Acute Stress (Z7)
                               │
             ┌─────────────────┴─────────────────┐
             ▼                                   ▼
   GOOGLE MAP-LIKE SATELLITE & CANVAS   RULE-BASED ENGINE
   • Real Satellite Tiles (Esri/OSM)    • Deterministic Agronomic Logic
   • GPS Current Location Auto-Center   • Trend Detection (NDVI/Moisture)
   • Translucent Risk Overlays          • Grounded AI Assistant Prompts
   • Tap Hit-Testing & Boundary Plot    • Dual Mode: Satellite & Vector
             │                                   │
             └─────────────────┬─────────────────┘
                               ▼
                   ACTIONABLE RECOMMENDATION
             "Zone 7 needs attention: Water stress risk detected"
                               │
                               ▼
                      FARMER ACTION LOOP
              [✓ Mark Irrigated] [✓ Mark Inspected]
                               │
                               ▼
                     UPDATED DIGITAL TWIN
              (Moisture 18% -> 32%, Risk -> Low, Health -> Good)
```

---

## 3. Field Creation & Boundary Mapping

1. **Tap-to-Plot Coordinate Boundary**:
   - The farmer taps 3 or more points on the map to define the boundary vertices of their field parcel.
   - Coordinates are stored as structured latitude/longitude pairs (`GeoPoint`).
2. **Geodesic Area Calculation**:
   - Uses the spherical polygon geodesic projection (Shoelace formula on planar equirectangular projection):
     $$x_i = (\lambda_i - \lambda_0) \cdot R \cos(\phi_{\text{avg}}), \quad y_i = (\phi_i - \phi_0) \cdot R$$
     $$\text{Area} = \frac{1}{2} \left| \sum_{i=0}^{n-1} (x_i y_{i+1} - x_{i+1} y_i) \right| \div 4046.86 \text{ (Acres)}$$
   - Clearly labels calculated acreage as approximate.
3. **Agronomic Metadata**:
   - Field name, primary crop (selected from the 49 ICAR catalog), variety, sowing date, and GPS center point.

---

## 4. Zone Generation (`FieldZoningEngine`)

- The field polygon is partitioned into a regular $3 \times 3$ grid of 9 management zones (`Z-01` to `Z-09`).
- Each zone maintains:
  - `id`: Unique identifier (e.g. `Z-01`, `Z-07`).
  - `boundary`: Local cell polygon.
  - `centerLat`, `centerLon`: Centroid for touch hit-testing and badge placement.
  - `areaAcres`: Sub-acreage parcel size (~0.58 acres each).
  - `crop`: Crop type.
- **Future-Ready Extensibility**: Designed so grid-based zoning can seamlessly be swapped with unsupervised spatial clustering (e.g. k-means on multi-spectral NDVI/soil variance).

---

## 5. Database Schema & Data Models

### `FarmerField`
* `id: String`
* `name: String`
* `crop_id: String`
* `crop_name: String`
* `crop_name_hi: String`
* `variety: String?`
* `area_acres: Double`
* `sowing_date: String`
* `boundary: List<GeoPoint>`
* `center_lat: Double`
* `center_lon: Double`

### `FieldZone`
* `id: String`
* `field_id: String`
* `zone_number: Int`
* `boundary: List<GeoPoint>`
* `area_acres: Double`

### `ZoneObservation`
* `id: String`
* `zone_id: String`
* `timestamp: Long`
* `date_string: String`
* `ndvi: Float` (0.0 to 1.0)
* `moisture: Int` (0% to 100%)
* `temperature: Float` (°C)
* `rainfall_mm: Float`
* `humidity: Int` (%)
* `crop_health: CropHealthStatus` (`EXCELLENT`, `GOOD`, `MODERATE`, `POOR`)
* `risk_level: RiskLevel` (`LOW`, `MEDIUM`, `HIGH`)
* `risk_type: String` (`WATER_STRESS`, `FUNGAL_RISK`, `HEAT_STRESS`, `NONE`)
* `source: String` (`Demonstration Data`, `Satellite`, `Weather API`, `Camera AI`, `Farmer Input`)

### `IrrigationEvent`
* `id: String`
* `zone_id: String`
* `timestamp: Long`
* `amount_mm: Float?`
* `notes: String?`
* `source: String`

---

## 6. Observation Pipeline & Provider Abstraction

Decoupled via `ObservationProvider`:
1. `DemoObservationProvider`: Provides deterministic, realistic multi-temporal data for Smart India Hackathon (SIH) demonstration.
2. `SatelliteObservationProvider`: Production stub for Sentinel-2 L2A BOA reflectance ($\text{NDVI} = \frac{B8 - B4}{B8 + B4}$).
3. `IoTObservationProvider`: Production stub for LoRaWAN/cellular telemetry soil moisture probes.

---

## 7. Weather Integration

- Directly linked to `DeviceLocationProvider` and Open-Meteo/ICAR live weather.
- Connects precipitation forecasts and ambient temperatures directly into the Digital Twin risk evaluation:
  - High heat ($>35^\circ\text{C}$) accelerates moisture depletion calculations.
  - Rain probability $\ge 60\%$ advises postponing scheduled irrigation to prevent root hypoxia.

---

## 8. Rule-Based Recommendation Engine

1. **Acute Water Stress**: $\text{Moisture} < 20\% \land \text{Temp} > 34^\circ\text{C} \land \text{Rain Prob} < 35\% \implies$ **High Risk Water Stress** &rarr; Action: `IRRIGATE`.
2. **Rain Delay Warning**: $\text{Moisture} < 23\% \land \text{Rain Prob} \ge 60\% \implies$ **Medium Risk** &rarr; Action: `HOLD_IRRIGATION`.
3. **Fungal Microclimate Alert**: $\text{Humidity} \ge 75\% \land \text{Temp} \in [22, 30]^\circ\text{C} \implies$ **Fungal Risk** &rarr; Action: `INSPECT`.
4. **NDVI Decline Trend**: $\Delta \text{NDVI} > 0.15$ over 3 observations &rarr; **Vegetation Health Decline** &rarr; Action: `INSPECT`.

---

## 9. Closed-Loop Action Feedback

When the farmer taps **"✓ Mark Irrigated (सिंचाई दर्ज करें)"**:
1. Logs `IrrigationEvent` into zone history.
2. Dynamically updates the Twin state:
   - Soil moisture jumps from $18\%$ to $32\%$.
   - NDVI stabilizes to $0.58$.
   - Health transitions to `Good`.
   - Risk transitions to `Low`.
   - Alert banner updates to: *"Zone 7 irrigated today — condition improving (ज़ोन 7 में सिंचाई दर्ज — स्थिति में सुधार)"*.

---

## 10. AI Role & Grounding

The AI Assistant does **NOT** invent raw sensor numbers. Instead:
- `FieldTwinRuleEngine.buildAssistantPrompt(...)` synthesizes structured zone facts.
- The AI Assistant explains the condition in natural conversational Hindi or English with zero hallucination.

---

## 11. Camera Disease Linkage

- When the farmer runs an on-device leaf disease diagnosis in `CameraScreen.kt`:
- A new button **"🌾 ज़ोन से जोड़ें (Assign to Zone)"** lets the farmer tag the scan to any zone (e.g. Zone 7).
- Persists an observation with `source = "Camera AI Scan"`.

---

## 12. Offline Capabilities

- Full local persistence via `FieldDigitalTwinRepository` (SharedPreferences + JSON with SQLite fallback).
- Farmers can view saved boundaries, zone states, historical trends, and record field actions offline in remote fields.

---

## 13. REST Endpoints (FastAPI)

* `GET /api/v1/fields`
* `POST /api/v1/fields`
* `GET /api/v1/fields/{field_id}`
* `POST /api/v1/fields/{field_id}/zones/generate`
* `GET /api/v1/fields/{field_id}/twin`
* `GET /api/v1/fields/zones/{zone_id}`
* `GET /api/v1/fields/zones/{zone_id}/observations`
* `POST /api/v1/fields/zones/{zone_id}/observations`
* `POST /api/v1/fields/zones/{zone_id}/irrigation`
* `POST /api/v1/fields/zones/{zone_id}/inspection`
* `GET /api/v1/fields/zones/{zone_id}/recommendations`
