import json
import os
from backend.app.db.session import SessionLocal, engine, Base
from backend.app.db.models import Crop, Disease, Scheme, Loan, KnowledgeFact
from backend.app.core.logging import logger

DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))), "data")

def seed_database():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()
    try:
        # Seed Crops
        crops_file = os.path.join(DATA_DIR, "verified_crops.json")
        if os.path.exists(crops_file):
            with open(crops_file, "r", encoding="utf-8") as f:
                crops_data = json.load(f)
            for c in crops_data:
                crop = Crop(
                    id=c["id"],
                    name_en=c["name_en"],
                    name_hi=c["name_hi"],
                    scientific_name=c.get("scientific_name"),
                    category=c.get("category"),
                    category_hi=c.get("category_hi"),
                    soil=c.get("soil"),
                    soil_hi=c.get("soil_hi"),
                    soil_ph=c.get("soil_ph"),
                    climate=c.get("climate"),
                    climate_hi=c.get("climate_hi"),
                    temperature=c.get("temperature"),
                    sowing_season=c.get("sowing_season"),
                    sowing_season_hi=c.get("sowing_season_hi"),
                    irrigation=c.get("irrigation"),
                    irrigation_hi=c.get("irrigation_hi"),
                    fertilizer=c.get("fertilizer"),
                    fertilizer_hi=c.get("fertilizer_hi"),
                    harvesting=c.get("harvesting"),
                    harvesting_hi=c.get("harvesting_hi"),
                    pests=c.get("pests"),
                    pests_hi=c.get("pests_hi"),
                    diseases=c.get("diseases"),
                    diseases_hi=c.get("diseases_hi"),
                    cultivation_tips=c.get("cultivation_tips"),
                    cultivation_tips_hi=c.get("cultivation_tips_hi"),
                    source=c.get("source"),
                    source_url=c.get("source_url")
                )
                db.merge(crop)
            logger.info(f"Seeded {len(crops_data)} crops.")

        # Seed Diseases
        diseases_file = os.path.join(DATA_DIR, "verified_diseases.json")
        if os.path.exists(diseases_file):
            with open(diseases_file, "r", encoding="utf-8") as f:
                diseases_data = json.load(f)
            for d in diseases_data:
                disease = Disease(
                    id=d["id"],
                    crop=d["crop"],
                    crop_hi=d["crop_hi"],
                    disease_name_en=d["disease_name_en"],
                    disease_name_hi=d["disease_name_hi"],
                    pathogen=d.get("pathogen"),
                    symptoms_en=d.get("symptoms_en"),
                    symptoms_hi=d.get("symptoms_hi"),
                    causes_en=d.get("causes_en"),
                    causes_hi=d.get("causes_hi"),
                    treatment_organic_en=d.get("treatment_organic_en"),
                    treatment_organic_hi=d.get("treatment_organic_hi"),
                    treatment_chemical_en=d.get("treatment_chemical_en"),
                    treatment_chemical_hi=d.get("treatment_chemical_hi"),
                    prevention_en=d.get("prevention_en"),
                    prevention_hi=d.get("prevention_hi"),
                    confidence_threshold=d.get("confidence_threshold", 0.70)
                )
                db.merge(disease)
            logger.info(f"Seeded {len(diseases_data)} diseases.")

        # Seed Schemes
        schemes_file = os.path.join(DATA_DIR, "verified_schemes.json")
        if os.path.exists(schemes_file):
            with open(schemes_file, "r", encoding="utf-8") as f:
                schemes_data = json.load(f)
            for s in schemes_data:
                scheme = Scheme(
                    id=s["id"],
                    name_en=s["name_en"],
                    name_hi=s["name_hi"],
                    category=s.get("category"),
                    category_hi=s.get("category_hi"),
                    ministry=s.get("ministry"),
                    benefits_en=s.get("benefits_en"),
                    benefits_hi=s.get("benefits_hi"),
                    eligibility_en=s.get("eligibility_en"),
                    eligibility_hi=s.get("eligibility_hi"),
                    application_process_en=s.get("application_process_en"),
                    application_process_hi=s.get("application_process_hi"),
                    official_url=s.get("official_url"),
                    source=s.get("source"),
                    last_verified=s.get("last_verified")
                )
                db.merge(scheme)
            logger.info(f"Seeded {len(schemes_data)} schemes.")

        # Seed Loans
        loans_file = os.path.join(DATA_DIR, "verified_loans.json")
        if os.path.exists(loans_file):
            with open(loans_file, "r", encoding="utf-8") as f:
                loans_data = json.load(f)
            for l in loans_data:
                loan = Loan(
                    id=l["id"],
                    bank_name=l["bank_name"],
                    bank_name_hi=l["bank_name_hi"],
                    loan_type=l["loan_type"],
                    loan_type_hi=l["loan_type_hi"],
                    purpose_en=l.get("purpose_en"),
                    purpose_hi=l.get("purpose_hi"),
                    interest_rate=l.get("interest_rate"),
                    interest_rate_hi=l.get("interest_rate_hi"),
                    max_limit=l.get("max_limit"),
                    max_limit_hi=l.get("max_limit_hi"),
                    eligibility_en=l.get("eligibility_en"),
                    eligibility_hi=l.get("eligibility_hi"),
                    documents_required=l.get("documents_required"),
                    documents_required_hi=l.get("documents_required_hi"),
                    official_url=l.get("official_url"),
                    source=l.get("source"),
                    last_verified=l.get("last_verified")
                )
                db.merge(loan)
            logger.info(f"Seeded {len(loans_data)} loans.")

        # Seed Knowledge Facts
        facts_file = os.path.join(DATA_DIR, "generated_training_questions.json")
        if os.path.exists(facts_file):
            with open(facts_file, "r", encoding="utf-8") as f:
                facts_data = json.load(f)
            if db.query(KnowledgeFact).count() < len(facts_data):
                db.query(KnowledgeFact).delete()
                for k in facts_data[:600]: # Seed representative 600 facts across all crops & intents
                    fact = KnowledgeFact(
                        intent=k["intent"],
                        crop_id=k.get("crop_id"),
                        language=k.get("lang"),
                        question=k["question"],
                        answer_en=k["answer_en"],
                        answer_hi=k["answer_hi"],
                        source=k["source"]
                    )
                    db.add(fact)
                logger.info("Seeded representative knowledge facts.")

        db.commit()

        # Seed Market Prices and Priority Crop Varieties
        from backend.app.db.ingest_datasets import run_ingestion
        run_ingestion()

    except Exception as e:
        logger.error(f"Error seeding database: {e}")
        db.rollback()
    finally:
        db.close()

if __name__ == "__main__":
    seed_database()

