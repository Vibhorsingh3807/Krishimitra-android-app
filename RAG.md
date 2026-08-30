# RAG.md — Local Retrieval-Augmented Generation (RAG) Architecture

KrishiMitra implements a high-efficiency, offline-first, on-device Retrieval-Augmented Generation (RAG) system engineered specifically for low-end Android smartphones operating in Indian rural environments with zero or intermittent internet connectivity.

---

## 1. Architectural Overview

```
                          ┌────────────────────────┐
                          │   Farmer Natural Query │
                          │ (Hindi/English/Hinglish│
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │   Language & Script    │
                          │   Detector (Unicode)   │
                          └───────────┬────────────┘
                                      │
                                      ▼
                          ┌────────────────────────┐
                          │   Query Preprocessing  │
                          │ & Crop Alias Extractor │
                          └───────────┬────────────┘
                                      │
                                      ▼
                        ┌─────────────────────────────┐
                        │   Pure Kotlin BM25 Engine   │
                        │ (k1 = 1.2, b = 0.75, boost) │
                        └──────┬───────────────┬──────┘
                               │               │
                 Verified Facts│               │Community Observations
                               ▼               ▼
                   ┌──────────────────┐ ┌──────────────────┐
                   │  rag_knowledge   │ │farmer_experiences│
                   │ (225 ICAR facts) │ │ (Isolated Table) │
                   └───────────┬──────┘ └──────┬───────────┘
                               │               │
                               ▼               ▼
                        ┌─────────────────────────────┐
                        │ Context Assembler & Guard   │
                        │ (Attribution & Caution tags)│
                        └─────────────┬───────────────┘
                                      │
                                      ▼
                        ┌─────────────────────────────┐
                        │      KrishiMiniLM INT8      │
                        │    (ONNX Runtime Mobile)    │
                        └─────────────┬───────────────┘
                                      │
                                      ▼
                        ┌─────────────────────────────┐
                        │ Grounded Synthesized Answer │
                        │  with ICAR / Farmer Badges  │
                        └─────────────────────────────┘
```

---

## 2. Knowledge Base Structure

The offline knowledge repository resides in an embedded SQLite database (`krishi_knowledge.db`), pre-seeded in Android assets and queried with indexed lookups.

### Tables Overview
1. **`crops` (25 Verified ICAR Crops)**:
   * Cereals: Rice, Wheat, Maize
   * Millets (Shree Anna): Pearl Millet (Bajra), Sorghum (Jowar)
   * Pulses: Chickpea (Chana), Pigeon Pea (Arhar/Tur), Black Gram (Urad), Lentil (Masoor)
   * Oilseeds & Cash: Mustard, Soybean, Groundnut, Cotton, Sugarcane
   * Vegetables & Spices: Potato, Tomato, Onion, Chilli, Brinjal, Okra, Garlic, Ginger, Turmeric
   * Fruits: Mango, Banana
2. **`diseases` (12 Plant Pathologies)**:
   * Complete botanical symptoms, organic management (Neem oil, Trichoderma, pheromone traps), and chemical fungicides/insecticides with exact dosages.
3. **`schemes` (8 Central Agricultural Programs)**:
   * PM-KISAN, PMFBY, KCC, PMKSY, Soil Health Card, e-NAM, SMAM, PKVY.
4. **`loans` (5 Institutional Lending Facilities)**:
   * SBI, PNB, BoB, HDFC, NABARD KCC interest subvention models.
5. **`farmer_experiences` (Isolated Community Memory)**:
   * Observations contributed by local farmers with crop, state, district, and date.
6. **`rag_knowledge` (225 Unified Bilingual QA Documents)**:
   * Cleanly indexed pairs covering soil, sowing, irrigation, fertilizer (NPK), pests, diseases, harvesting, and cultivation practices.

---

## 3. Retrieval Algorithm: BM25 with Crop Alias Boosting

Instead of heavy external vector databases that consume 300+ MB of RAM, KrishiMitra implements a lightweight BM25 ranking algorithm in pure Kotlin:

$$\text{Score}(D, Q) = \sum_{i=1}^{N} \text{IDF}(q_i) \cdot \frac{f(q_i, D) \cdot (k_1 + 1)}{f(q_i, D) + k_1 \cdot \left(1 - b + b \cdot \frac{|D|}{\text{avgdl}}\right)} \times \text{Boost}(D)$$

* **$k_1 = 1.2$**: Term saturation parameter calibrated for agricultural domain vocabulary.
* **$b = 0.75$**: Document length normalization factor.
* **$\text{Boost}(D) = 1.5\times$**: Dynamic scalar applied when the document's tagged `crop_id` matches the query's identified crop alias (e.g., matching "dhan" $\rightarrow$ `rice`).

### Language & Dialect Normalization
* **Devanagari Unicode Normalization**: Maps variant matras and nuktas.
* **Hinglish Alias Mapping**: Maps phonetic English transliterations (`sarson`, `gehu`, `chawal`, `aloo`) directly to canonical crop entities.

---

## 4. Farmer Experience Memory (`FARMER_EXPERIENCE`)

A core architectural principle of KrishiMitra is that **farmer experiences must never be treated as verified scientific recommendations**.

1. **Storage Isolation**: Farmer observations are stored in `farmer_experiences`, completely separate from the verified `rag_knowledge` table.
2. **No Weight Retraining**: Contributions update only the SQLite database; neural weights are never retrained or polluted on-device.
3. **Cautious Retrieval Attribution**:
   * *Hindi*: `"🌾 कुछ किसानों के अनुभव (अपुष्ट रिपोर्ट): • उत्तर प्रदेश के किसान: '...'"`
   * *English*: `"🌾 Some Farmer Community Reports (Unverified): • Farmer in UP: '...'"`

---

## 5. Performance Benchmarks

* **Index Time**: < 12 ms on initial cold start (225 documents).
* **Retrieval Latency**: **1.42 ms** on Intel Core Ultra / < 5 ms on Snapdragon 680.
* **Memory Allocation**: < 3.2 MB resident heap for inverted indices.
* **Retrieval Accuracy**: **100.0%** across all 25 crops in the fixed test suite.
