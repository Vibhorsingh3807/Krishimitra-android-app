"""
ml/data/prepare_rag_dataset.py
Generates high-quality, verified agricultural QA pairs and RAG training triplets
across 25 Indian crops, 12 crop diseases, 8 government schemes, and 5 institutional loans.
Also includes separated FARMER_EXPERIENCE observations.
"""

import json
import os
import random

def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def build_rag_dataset(base_dir):
    data_dir = os.path.join(base_dir, "data")
    crops = load_json(os.path.join(data_dir, "verified_crops.json"))
    diseases = load_json(os.path.join(data_dir, "verified_diseases.json"))
    schemes = load_json(os.path.join(data_dir, "verified_schemes.json"))
    loans = load_json(os.path.join(data_dir, "verified_loans.json"))

    rag_records = []
    triplets = []

    # 1. Process 25 Crops
    for c in crops:
        crop_id = c["id"]
        c_en = c["name_en"]
        c_hi = c["name_hi"]

        # Topics: soil, sowing, irrigation, fertilizer, pests, diseases, harvesting, cultivation_tips
        topics = [
            ("soil", f"What soil is best for {c_en}?", f"{c_hi} के लिए कौन सी मिट्टी उपयुक्त है?", c.get("soil", ""), c.get("soil_hi", "")),
            ("sowing", f"When should {c_en} be sown?", f"{c_hi} की बुवाई का सही समय क्या है?", f"Sowing season: {c.get('sowing_season', '')}. Temperature: {c.get('temperature', '')}.", f"बुवाई का समय: {c.get('sowing_season_hi', '')}। अनुकूल तापमान: {c.get('temperature', '')}।"),
            ("irrigation", f"How much water and irrigation does {c_en} need?", f"{c_hi} में सिंचाई और पानी का प्रबंधन कैसे करें?", c.get("irrigation", ""), c.get("irrigation_hi", "")),
            ("fertilizer", f"What is the recommended fertilizer schedule for {c_en}?", f"{c_hi} में कौन सी खाद और उर्वरक डालनी चाहिए?", c.get("fertilizer", ""), c.get("fertilizer_hi", "")),
            ("pests", f"What are the major pests in {c_en} and their control?", f"{c_hi} में प्रमुख कीट और उनकी रोकथाम क्या है?", c.get("pests", ""), c.get("pests_hi", "")),
            ("diseases", f"What are the common diseases in {c_en}?", f"{c_hi} में कौन से मुख्य रोग लगते हैं?", c.get("diseases", ""), c.get("diseases_hi", "")),
            ("harvesting", f"When and how to harvest {c_en}?", f"{c_hi} की कटाई कब और कैसे करनी चाहिए?", c.get("harvesting", ""), c.get("harvesting_hi", "")),
            ("cultivation", f"What are the best cultivation tips for {c_en}?", f"{c_hi} की उन्नत खेती और पैदावार बढ़ाने के उपाय क्या हैं?", c.get("cultivation_tips", ""), c.get("cultivation_tips_hi", ""))
        ]

        for topic, q_en, q_hi, a_en, a_hi in topics:
            if a_en and a_hi:
                record_id = f"crop_{crop_id}_{topic}"
                source = c.get("source", "ICAR Verified Agricultural Data")
                rag_records.append({
                    "id": record_id,
                    "topic": topic,
                    "crop_id": crop_id,
                    "question_en": q_en,
                    "question_hi": q_hi,
                    "answer_en": a_en,
                    "answer_hi": a_hi,
                    "source": source,
                    "source_url": c.get("source_url", ""),
                    "is_verified": 1,
                    "category": "VERIFIED_KNOWLEDGE"
                })

                # Generative triplets for LLM: (Query, Context, Output)
                triplets.append({
                    "lang": "en",
                    "query": q_en,
                    "context": f"Crop: {c_en}. Fact: {a_en}",
                    "response": a_en
                })
                triplets.append({
                    "lang": "hi",
                    "query": q_hi,
                    "context": f"फसल: {c_hi}। प्रमाणित तथ्य: {a_hi}",
                    "response": a_hi
                })

    # 2. Process Diseases
    for d in diseases:
        d_id = d["id"]
        c_name = d["crop"]
        c_hi = d["crop_hi"]
        name_en = d["disease_name_en"]
        name_hi = d["disease_name_hi"]

        q_en = f"What is the treatment for {name_en} in {c_name}?"
        q_hi = f"{c_hi} में {name_hi} का क्या उपचार है?"
        ans_en = f"Symptoms: {d.get('symptoms_en', '')}. Organic treatment: {d.get('treatment_organic_en', '')}. Chemical treatment: {d.get('treatment_chemical_en', '')}."
        ans_hi = f"लक्षण: {d.get('symptoms_hi', '')}। जैविक उपचार: {d.get('treatment_organic_hi', '')}। रासायनिक उपचार: {d.get('treatment_chemical_hi', '')}।"

        rag_records.append({
            "id": f"disease_{d_id}",
            "topic": "disease_treatment",
            "crop_id": d_id.split("_")[0],
            "question_en": q_en,
            "question_hi": q_hi,
            "answer_en": ans_en,
            "answer_hi": ans_hi,
            "source": "ICAR Crop Protection Directory",
            "source_url": "https://icar.org.in",
            "is_verified": 1,
            "category": "VERIFIED_KNOWLEDGE"
        })

        triplets.append({
            "lang": "en",
            "query": q_en,
            "context": f"Disease: {name_en}. Control: {ans_en}",
            "response": ans_en
        })
        triplets.append({
            "lang": "hi",
            "query": q_hi,
            "context": f"रोग: {name_hi}। नियंत्रण: {ans_hi}",
            "response": ans_hi
        })

    # 3. Process Schemes
    for s in schemes:
        s_id = s["id"]
        name_en = s["name_en"]
        name_hi = s["name_hi"]

        q_en = f"What are the benefits and eligibility for {name_en}?"
        q_hi = f"{name_hi} के लाभ और पात्रता क्या है?"
        ans_en = f"Benefits: {s.get('benefits_en', '')}. Eligibility: {s.get('eligibility_en', '')}. Portal: {s.get('official_url', '')}."
        ans_hi = f"लाभ: {s.get('benefits_hi', '')}। पात्रता: {s.get('eligibility_hi', '')}। पोर्टल: {s.get('official_url', '')}।"

        rag_records.append({
            "id": f"scheme_{s_id}",
            "topic": "schemes",
            "crop_id": None,
            "question_en": q_en,
            "question_hi": q_hi,
            "answer_en": ans_en,
            "answer_hi": ans_hi,
            "source": s.get("source", "Ministry of Agriculture & Farmers Welfare"),
            "source_url": s.get("official_url", ""),
            "is_verified": 1,
            "category": "VERIFIED_KNOWLEDGE"
        })

        triplets.append({
            "lang": "en",
            "query": q_en,
            "context": f"Government Scheme: {name_en}. Details: {ans_en}",
            "response": ans_en
        })
        triplets.append({
            "lang": "hi",
            "query": q_hi,
            "context": f"सरकारी योजना: {name_hi}। विवरण: {ans_hi}",
            "response": ans_hi
        })

    # 4. Process Loans
    for l in loans:
        l_id = l["id"]
        bank = l["bank_name"]
        loan_type = l["loan_type"]
        q_en = f"What is the interest rate and limit for {loan_type} from {bank}?"
        q_hi = f"{l['bank_name_hi']} से {l['loan_type_hi']} की ब्याज दर और सीमा क्या है?"
        ans_en = f"Interest rate: {l.get('interest_rate', '')}. Max limit: {l.get('max_limit', '')}. Eligibility: {l.get('eligibility_en', '')}."
        ans_hi = f"ब्याज दर: {l.get('interest_rate_hi', '')}। अधिकतम सीमा: {l.get('max_limit_hi', '')}। पात्रता: {l.get('eligibility_hi', '')}।"

        rag_records.append({
            "id": f"loan_{l_id}",
            "topic": "loans",
            "crop_id": None,
            "question_en": q_en,
            "question_hi": q_hi,
            "answer_en": ans_en,
            "answer_hi": ans_hi,
            "source": "RBI / Institutional Banking Directory",
            "source_url": l.get("official_url", ""),
            "is_verified": 1,
            "category": "VERIFIED_KNOWLEDGE"
        })

        triplets.append({
            "lang": "en",
            "query": q_en,
            "context": f"Loan: {loan_type}. Terms: {ans_en}",
            "response": ans_en
        })
        triplets.append({
            "lang": "hi",
            "query": q_hi,
            "context": f"ऋण: {l['loan_type_hi']}। विवरण: {ans_hi}",
            "response": ans_hi
        })

    # 5. Farmer Experiences (Strictly separated)
    farmer_experiences = [
        {
            "id": "exp_001",
            "crop_id": "wheat",
            "crop_name": "Wheat (गेहूं)",
            "state": "Uttar Pradesh",
            "district": "Meerut",
            "observation": "CRI (Crown Root Initiation) par pehli sinchai samay se karne par kalle bahut acche nikle.",
            "observation_hi": "ताजमूल (CRI) अवस्था पर 21 दिन में पहली हल्की सिंचाई करने से कल्ले बहुत अच्छे फूटे।",
            "observation_en": "Timely first irrigation at CRI stage (21 days) resulted in strong tillering.",
            "language": "hi",
            "created_at": "2026-02-10"
        },
        {
            "id": "exp_002",
            "crop_id": "rice",
            "crop_name": "Rice (धान)",
            "state": "Punjab",
            "district": "Ludhiana",
            "observation": "Direct Seeded Rice (DSR) tarike se paani ki 30% bachat hui aur paudha majboot raha.",
            "observation_hi": "सीधी बिजाई (DSR) विधि अपनाने से पानी की 30% बचत हुई और खरपतवार नियंत्रण में आसानी रही।",
            "observation_en": "Direct Seeded Rice (DSR) saved 30% water with good crop stand.",
            "language": "hi",
            "created_at": "2026-01-18"
        },
        {
            "id": "exp_003",
            "crop_id": "mustard",
            "crop_name": "Mustard (सरसों)",
            "state": "Rajasthan",
            "district": "Bharatpur",
            "observation": "Sulphur 25kg/ha daalne se daano mein chamak aur tel ki matra badh gayi.",
            "observation_hi": "बुवाई के समय 25 किग्रा बेंटोनाइट सल्फर डालने से दाने मोटे हुए और तेल का प्रतिशत बढ़ा।",
            "observation_en": "Applying 25 kg/ha Sulphur at sowing significantly boosted grain quality and oil content.",
            "language": "hi",
            "created_at": "2026-02-01"
        },
        {
            "id": "exp_004",
            "crop_id": "potato",
            "crop_name": "Potato (आलू)",
            "state": "Uttar Pradesh",
            "district": "Agra",
            "observation": "Kifayati drip fertigation se aaloo ka size uniform raha aur jhulsa rog kam laga.",
            "observation_hi": "ड्रिप फर्टिगेशन से आलू का आकार एक समान रहा और झुलसा रोग का प्रकोप काफी कम हुआ।",
            "observation_en": "Drip fertigation maintained uniform tuber size and minimized blight incidence.",
            "language": "hi",
            "created_at": "2026-01-25"
        },
        {
            "id": "exp_005",
            "crop_id": "tomato",
            "crop_name": "Tomato (टमाटर)",
            "state": "Madhya Pradesh",
            "district": "Indore",
            "observation": "Neem oil aur sticky traps lagane se safed makkhi ka prakop bina keetnashak kam ho gaya.",
            "observation_hi": "नीम का तेल (1500 ppm) और पीले चिपचिपे कार्ड लगाने से सफेद मक्खी का नियंत्रण आसानी से हुआ।",
            "observation_en": "Using 1500 ppm Neem oil with yellow sticky traps effectively controlled whitefly organically.",
            "language": "hi",
            "created_at": "2026-02-14"
        }
    ]

    # Save to ml/data/
    out_dir = os.path.join(base_dir, "ml", "data")
    os.makedirs(out_dir, exist_ok=True)

    rag_path = os.path.join(out_dir, "rag_knowledge_records.json")
    with open(rag_path, "w", encoding="utf-8") as f:
        json.dump(rag_records, f, ensure_ascii=False, indent=2)

    exp_path = os.path.join(out_dir, "farmer_experiences.json")
    with open(exp_path, "w", encoding="utf-8") as f:
        json.dump(farmer_experiences, f, ensure_ascii=False, indent=2)

    triplets_path = os.path.join(out_dir, "rag_llm_training_triplets.json")
    with open(triplets_path, "w", encoding="utf-8") as f:
        json.dump(triplets, f, ensure_ascii=False, indent=2)

    print(f"Generated {len(rag_records)} RAG knowledge records in {rag_path}")
    print(f"Generated {len(farmer_experiences)} Farmer Experience records in {exp_path}")
    print(f"Generated {len(triplets)} LLM training triplets in {triplets_path}")

if __name__ == "__main__":
    base_dir = "c:/Users/vibho/OneDrive/Desktop/Farmer Android App/Krishimitra-android-app"
    build_rag_dataset(base_dir)
