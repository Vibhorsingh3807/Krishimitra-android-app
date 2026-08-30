"""
ml/scripts/build_full_database.py
Builds the comprehensive SQLite database krishi_knowledge.db including:
- crops (25 crops)
- diseases (12 diseases)
- schemes (8 schemes)
- loans (5 loans)
- farmer_experiences (community observation memory)
- rag_knowledge (225 bilingual RAG records)
Then deploys directly to android/app/src/main/assets/krishi_knowledge.db.
"""

import sqlite3
import json
import os
import shutil

def build_database(base_dir):
    data_dir = os.path.join(base_dir, "data")
    ml_data_dir = os.path.join(base_dir, "ml", "data")
    db_path = os.path.join(base_dir, "krishimitra.db")
    assets_db = os.path.join(base_dir, "android", "app", "src", "main", "assets", "krishi_knowledge.db")

    if os.path.exists(db_path):
        os.remove(db_path)

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # 1. Crops Table
    cur.execute("""
    CREATE TABLE crops (
        id TEXT PRIMARY KEY,
        name_en TEXT NOT NULL,
        name_hi TEXT NOT NULL,
        scientific_name TEXT,
        category TEXT,
        category_hi TEXT,
        soil TEXT,
        soil_hi TEXT,
        soil_ph TEXT,
        climate TEXT,
        climate_hi TEXT,
        temperature TEXT,
        sowing_season TEXT,
        sowing_season_hi TEXT,
        irrigation TEXT,
        irrigation_hi TEXT,
        fertilizer TEXT,
        fertilizer_hi TEXT,
        harvesting TEXT,
        harvesting_hi TEXT,
        pests TEXT,
        pests_hi TEXT,
        diseases TEXT,
        diseases_hi TEXT,
        cultivation_tips TEXT,
        cultivation_tips_hi TEXT,
        source TEXT,
        source_url TEXT,
        updated_at TEXT DEFAULT '2026-08-30 00:00:00'
    )
    """)

    crops = json.load(open(os.path.join(data_dir, "verified_crops.json"), encoding="utf-8"))
    for c in crops:
        cur.execute("""
        INSERT INTO crops VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, (
            c["id"], c["name_en"], c["name_hi"], c.get("scientific_name"),
            c.get("category"), c.get("category_hi"), c.get("soil"), c.get("soil_hi"),
            c.get("soil_ph"), c.get("climate"), c.get("climate_hi"), c.get("temperature"),
            c.get("sowing_season"), c.get("sowing_season_hi"), c.get("irrigation"), c.get("irrigation_hi"),
            c.get("fertilizer"), c.get("fertilizer_hi"), c.get("harvesting"), c.get("harvesting_hi"),
            c.get("pests"), c.get("pests_hi"), c.get("diseases"), c.get("diseases_hi"),
            c.get("cultivation_tips"), c.get("cultivation_tips_hi"), c.get("source"), c.get("source_url"),
            "2026-08-30 00:00:00"
        ))

    # 2. Diseases Table
    cur.execute("""
    CREATE TABLE diseases (
        id TEXT PRIMARY KEY,
        crop TEXT NOT NULL,
        crop_hi TEXT NOT NULL,
        disease_name_en TEXT NOT NULL,
        disease_name_hi TEXT NOT NULL,
        pathogen TEXT,
        symptoms_en TEXT,
        symptoms_hi TEXT,
        causes_en TEXT,
        causes_hi TEXT,
        treatment_organic_en TEXT,
        treatment_organic_hi TEXT,
        treatment_chemical_en TEXT,
        treatment_chemical_hi TEXT,
        prevention_en TEXT,
        prevention_hi TEXT,
        confidence_threshold REAL DEFAULT 0.70,
        updated_at TEXT DEFAULT '2026-08-30 00:00:00'
    )
    """)

    diseases = json.load(open(os.path.join(data_dir, "verified_diseases.json"), encoding="utf-8"))
    for d in diseases:
        cur.execute("""
        INSERT INTO diseases VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, (
            d["id"], d["crop"], d["crop_hi"], d["disease_name_en"], d["disease_name_hi"],
            d.get("pathogen"), d.get("symptoms_en"), d.get("symptoms_hi"), d.get("causes_en"), d.get("causes_hi"),
            d.get("treatment_organic_en"), d.get("treatment_organic_hi"), d.get("treatment_chemical_en"), d.get("treatment_chemical_hi"),
            d.get("prevention_en"), d.get("prevention_hi"), d.get("confidence_threshold", 0.70),
            "2026-08-30 00:00:00"
        ))

    # 3. Schemes Table
    cur.execute("""
    CREATE TABLE schemes (
        id TEXT PRIMARY KEY,
        name_en TEXT NOT NULL,
        name_hi TEXT NOT NULL,
        category TEXT,
        category_hi TEXT,
        ministry TEXT,
        benefits_en TEXT,
        benefits_hi TEXT,
        eligibility_en TEXT,
        eligibility_hi TEXT,
        application_process_en TEXT,
        application_process_hi TEXT,
        official_url TEXT,
        source TEXT,
        last_verified TEXT,
        updated_at TEXT DEFAULT '2026-08-30 00:00:00'
    )
    """)

    schemes = json.load(open(os.path.join(data_dir, "verified_schemes.json"), encoding="utf-8"))
    for s in schemes:
        cur.execute("""
        INSERT INTO schemes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, (
            s["id"], s["name_en"], s["name_hi"], s.get("category"), s.get("category_hi"),
            s.get("ministry"), s.get("benefits_en"), s.get("benefits_hi"), s.get("eligibility_en"), s.get("eligibility_hi"),
            s.get("application_process_en"), s.get("application_process_hi"), s.get("official_url"), s.get("source"), s.get("last_verified"),
            "2026-08-30 00:00:00"
        ))

    # 4. Loans Table
    cur.execute("""
    CREATE TABLE loans (
        id TEXT PRIMARY KEY,
        bank_name TEXT NOT NULL,
        bank_name_hi TEXT NOT NULL,
        loan_type TEXT NOT NULL,
        loan_type_hi TEXT NOT NULL,
        purpose_en TEXT,
        purpose_hi TEXT,
        interest_rate TEXT,
        interest_rate_hi TEXT,
        max_limit TEXT,
        max_limit_hi TEXT,
        eligibility_en TEXT,
        eligibility_hi TEXT,
        documents_required TEXT,
        documents_required_hi TEXT,
        official_url TEXT,
        source TEXT,
        last_verified TEXT,
        updated_at TEXT DEFAULT '2026-08-30 00:00:00'
    )
    """)

    loans = json.load(open(os.path.join(data_dir, "verified_loans.json"), encoding="utf-8"))
    for l in loans:
        cur.execute("""
        INSERT INTO loans VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """, (
            l["id"], l["bank_name"], l["bank_name_hi"], l["loan_type"], l["loan_type_hi"],
            l.get("purpose_en"), l.get("purpose_hi"), l.get("interest_rate"), l.get("interest_rate_hi"),
            l.get("max_limit"), l.get("max_limit_hi"), l.get("eligibility_en"), l.get("eligibility_hi"),
            l.get("documents_required"), l.get("documents_required_hi"), l.get("official_url"), l.get("source"), l.get("last_verified"),
            "2026-08-30 00:00:00"
        ))

    # 5. Farmer Experience Table
    cur.execute("""
    CREATE TABLE farmer_experiences (
        id TEXT PRIMARY KEY,
        crop_id TEXT NOT NULL,
        crop_name TEXT NOT NULL,
        state TEXT NOT NULL,
        district TEXT,
        observation TEXT NOT NULL,
        observation_hi TEXT,
        observation_en TEXT,
        language TEXT DEFAULT 'hi',
        created_at TEXT
    )
    """)

    exp_file = os.path.join(ml_data_dir, "farmer_experiences.json")
    if os.path.exists(exp_file):
        experiences = json.load(open(exp_file, encoding="utf-8"))
        for e in experiences:
            cur.execute("""
            INSERT INTO farmer_experiences VALUES (?,?,?,?,?,?,?,?,?,?)
            """, (
                e["id"], e["crop_id"], e["crop_name"], e["state"], e.get("district"),
                e["observation"], e.get("observation_hi"), e.get("observation_en"),
                e.get("language", "hi"), e.get("created_at")
            ))

    # 6. Unified RAG Knowledge Table
    cur.execute("""
    CREATE TABLE rag_knowledge (
        id TEXT PRIMARY KEY,
        topic TEXT NOT NULL,
        crop_id TEXT,
        question_en TEXT NOT NULL,
        question_hi TEXT NOT NULL,
        answer_en TEXT NOT NULL,
        answer_hi TEXT NOT NULL,
        source TEXT NOT NULL,
        source_url TEXT,
        is_verified INTEGER DEFAULT 1,
        category TEXT DEFAULT 'VERIFIED_KNOWLEDGE'
    )
    """)

    rag_file = os.path.join(ml_data_dir, "rag_knowledge_records.json")
    if os.path.exists(rag_file):
        rag_records = json.load(open(rag_file, encoding="utf-8"))
        for r in rag_records:
            cur.execute("""
            INSERT INTO rag_knowledge VALUES (?,?,?,?,?,?,?,?,?,?,?)
            """, (
                r["id"], r["topic"], r.get("crop_id"), r["question_en"], r["question_hi"],
                r["answer_en"], r["answer_hi"], r["source"], r.get("source_url"),
                r.get("is_verified", 1), r.get("category", "VERIFIED_KNOWLEDGE")
            ))

    # Create search indexes
    cur.execute("CREATE INDEX idx_crops_name ON crops(name_en, name_hi)")
    cur.execute("CREATE INDEX idx_rag_crop_topic ON rag_knowledge(crop_id, topic)")
    cur.execute("CREATE INDEX idx_farmer_exp_crop ON farmer_experiences(crop_id)")

    conn.commit()
    conn.close()

    db_size = os.path.getsize(db_path) / 1024
    print(f"Created SQLite database: {db_path} ({db_size:.1f} KB)")

    # Copy to Android Assets
    os.makedirs(os.path.dirname(assets_db), exist_ok=True)
    shutil.copyfile(db_path, assets_db)
    print(f"Deployed to Android assets: {assets_db}")

if __name__ == "__main__":
    base = "c:/Users/vibho/OneDrive/Desktop/Farmer Android App/Krishimitra-android-app"
    build_database(base)
