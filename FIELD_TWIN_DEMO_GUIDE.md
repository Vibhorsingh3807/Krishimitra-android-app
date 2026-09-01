# 🏆 Field Digital Twin — Smart India Hackathon (SIH) Demo Guide

This guide gives the exact step-by-step workflow to showcase the **Field Digital Twin** to SIH evaluators.

---

## 🎯 The Core Elevator Pitch for Judges

> *"Respected Judges, most precision agriculture tools today offer only static satellite maps or generic city-level weather forecasts. KrishiMitra introduces India's first farmer-centric **Field Digital Twin**.*
>
> *We partition the farmer's physical field into 9 active management zones. By fusing microclimate data, multi-temporal vegetation indices, and farmer actions into a living digital model, we demonstrate a closed loop:*
>
> **Real Field $\longrightarrow$ Digital Twin $\longrightarrow$ Trend Analysis $\longrightarrow$ Targeted Recommendation $\longrightarrow$ Farmer Action $\longrightarrow$ Updated Twin.**
>
> *Notice that our architecture clearly labels simulated demonstration data so you know our underlying AI and geospatial pipelines are authentic and ready for live Sentinel-2 and IoT integration."*

---

## 🎬 Step-by-Step Live Demonstration Script

### Step 1: Open the Field Digital Twin
1. Launch KrishiMitra on your Android device.
2. On the **Home Screen**, point out the primary green feature card:
   👉 **"🌾 मेरा डिजिटल खेत (Field Digital Twin)"** / **"Field Digital Twin"**.
3. Tap the card.
4. Point out the top header:
   - Field Name: **"My Wheat Field"** (5.2 Acres, Wheat, HD-2967).
   - Provenance tag: **"Source: Demonstration Data (SIH Prototype)"**.

### Step 2: Showcase the Interactive 9-Zone Map
1. Show the centerpiece Canvas Map:
   - Point out the 9 individual grid zones (`Z-01` to `Z-09`).
   - Explain the color-coded health indicators:
     - 🟢 **Green (Healthy)**: Zones 1, 2, 4, 6.
     - 🟡 **Yellow (Moderate/Monitor)**: Zones 3, 5, 8, 9.
     - 🔴 **Red (Acute Water Stress)**: **Zone 7**.
2. Point out the compact field metrics bar below the map:
   - Crop: **Wheat**
   - Average Moisture: **24%**
   - Average NDVI: **0.61**
   - Attention Zones: **2 Zones**

### Step 3: Highlight the Critical Alert
1. Show the prominent red alert card:
   - **"⚠️ Water Stress Alert: Immediate Irrigation Needed"** / **"पानी की कमी की चेतावनी: तत्काल सिंचाई आवश्यक"**.
   - Explanation: *"Zone 7 soil moisture has dropped to 18% under 36°C heat with no imminent rain."*
2. Tap **"ज़ोन 7 जांचें व सिंचाई करें → (Inspect Zone 7)"** or tap the **🔴 Z-07 polygon** directly on the map.

### Step 4: Inspect Zone 7 Multi-Temporal Trend
1. Point out the current state gauges:
   - 💧 **Soil Moisture**: `18% ↓` (Critical).
   - 🌿 **NDVI Index**: `0.49 ↓` (Vegetation vigor declining).
   - 🌡️ **Temperature**: `36°C` (Heat stress).
2. Point out the **Multi-Temporal Trend Sparkline Chart**:
   - Trace the decline over 30 days:
     - 01 Aug: `32%`
     - 10 Aug: `28%`
     - 20 Aug: `22%`
     - Today: `18%`
3. Explain: *"The Digital Twin identifies trends over time rather than relying on a single snapshot."*

### Step 5: Execute the Closed-Loop Action
1. Scroll to the **Farmer Action Loop** section.
2. Tap **"✓ सिंचाई दर्ज करें (Mark Irrigated)"**.
3. A modal opens with default 35mm irrigation amount.
4. Tap **"सहेजें व ट्विन अपडेट करें (Save & Update Twin)"**.
5. **Watch the instant Digital Twin transformation**:
   - Soil moisture jumps from `18%` &rarr; **`32%`** 💧.
   - Crop Health changes from `Poor` &rarr; **`Good`** 🟢.
   - Risk Level changes from `HIGH` &rarr; **`LOW`**.
   - The alert clears!
   - Top banner updates: *"Zone Z-07: Irrigated 35mm today — soil moisture restored to 32%"*!
6. Explain to judges: *"This completes the closed loop: the farmer took action in the physical field, and the digital twin instantly evolved to reflect the restored moisture."*

### Step 6: Grounded AI Assistant Advisory
1. In the Zone detail view, tap **"इस ज़ोन पर AI कृषि साथी से सलाह लें (Ask AI Assistant)"**.
2. Show that the structured zone metrics (Moisture 32%, NDVI 0.58, Temp 31°C) are passed directly into the AI Assistant.
3. The AI gives contextual agronomic advice in conversational Hindi or English without hallucinating raw numbers.

### Step 7: Leaf Disease Scan Integration
1. Go back to **Scan Crop (कैमरा)**.
2. Run a leaf scan (or tap one of the quick test chips like "पीला रतुआ").
3. At the bottom of the diagnosis, tap **"🌾 ज़ोन से जोड़ें (Assign to Zone)"**.
4. Select **"Zone 7"**.
5. Show how camera AI disease observations are directly tagged into the Digital Twin history!

### Step 8: Adding a New Field (Boundary Drawing)
1. Tap **"+ नया खेत (+ Add Field)"**.
2. Tap 4 points on the interactive map canvas to draw a polygon.
3. Point out that the geodesic area calculates dynamically in real-time (e.g. `5.2 एकड़`).
4. Select crop from the 49 ICAR catalog.
5. Tap **"खेत सहेजें व डिजिटल ट्विन बनाएं"** &rarr; generates 9 digital zones instantly!

---

## 🛡️ Key Technical Takeaways for Judges

1. **Mobile-First & Offline-Ready**: Works 100% offline using local SQLite / SharedPreferences cache and low-memory Canvas graphics (< 2MB RAM).
2. **Deterministic & Credible**: Clearly identifies demonstration data so the architecture is auditable.
3. **Plug-and-Play Providers**: Uses `ObservationProvider` interface designed to ingest live Sentinel-2 Copernicus API data and IoT probes.
4. **Zero Proprietary Map Lock-In**: High-performance Compose Canvas vector rendering avoiding paid map API bills.
