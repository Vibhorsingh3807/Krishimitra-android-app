import datetime
from sqlalchemy import Column, String, Text, Float, DateTime, Integer
from backend.app.db.session import Base

class Crop(Base):
    __tablename__ = "crops"

    id = Column(String(50), primary_key=True, index=True)
    name_en = Column(String(100), nullable=False)
    name_hi = Column(String(100), nullable=False)
    scientific_name = Column(String(100))
    category = Column(String(50))
    category_hi = Column(String(50))
    soil = Column(Text)
    soil_hi = Column(Text)
    soil_ph = Column(String(50))
    climate = Column(Text)
    climate_hi = Column(Text)
    temperature = Column(String(50))
    sowing_season = Column(String(100))
    sowing_season_hi = Column(String(100))
    irrigation = Column(Text)
    irrigation_hi = Column(Text)
    fertilizer = Column(Text)
    fertilizer_hi = Column(Text)
    harvesting = Column(Text)
    harvesting_hi = Column(Text)
    pests = Column(Text)
    pests_hi = Column(Text)
    diseases = Column(Text)
    diseases_hi = Column(Text)
    cultivation_tips = Column(Text)
    cultivation_tips_hi = Column(Text)
    source = Column(String(200))
    source_url = Column(String(250))
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

class Disease(Base):
    __tablename__ = "diseases"

    id = Column(String(50), primary_key=True, index=True)
    crop = Column(String(50), nullable=False)
    crop_hi = Column(String(50), nullable=False)
    disease_name_en = Column(String(100), nullable=False)
    disease_name_hi = Column(String(100), nullable=False)
    pathogen = Column(String(100))
    symptoms_en = Column(Text)
    symptoms_hi = Column(Text)
    causes_en = Column(Text)
    causes_hi = Column(Text)
    treatment_organic_en = Column(Text)
    treatment_organic_hi = Column(Text)
    treatment_chemical_en = Column(Text)
    treatment_chemical_hi = Column(Text)
    prevention_en = Column(Text)
    prevention_hi = Column(Text)
    confidence_threshold = Column(Float, default=0.70)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

class Scheme(Base):
    __tablename__ = "schemes"

    id = Column(String(50), primary_key=True, index=True)
    name_en = Column(String(150), nullable=False)
    name_hi = Column(String(150), nullable=False)
    category = Column(String(50))
    category_hi = Column(String(50))
    ministry = Column(String(200))
    benefits_en = Column(Text)
    benefits_hi = Column(Text)
    eligibility_en = Column(Text)
    eligibility_hi = Column(Text)
    application_process_en = Column(Text)
    application_process_hi = Column(Text)
    official_url = Column(String(250))
    source = Column(String(200))
    last_verified = Column(String(50))
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

class Loan(Base):
    __tablename__ = "loans"

    id = Column(String(50), primary_key=True, index=True)
    bank_name = Column(String(150), nullable=False)
    bank_name_hi = Column(String(150), nullable=False)
    loan_type = Column(String(150), nullable=False)
    loan_type_hi = Column(String(150), nullable=False)
    purpose_en = Column(Text)
    purpose_hi = Column(Text)
    interest_rate = Column(String(100))
    interest_rate_hi = Column(String(100))
    max_limit = Column(String(100))
    max_limit_hi = Column(String(100))
    eligibility_en = Column(Text)
    eligibility_hi = Column(Text)
    documents_required = Column(Text)
    documents_required_hi = Column(Text)
    official_url = Column(String(250))
    source = Column(String(200))
    last_verified = Column(String(50))
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

class KnowledgeFact(Base):
    __tablename__ = "knowledge_facts"

    id = Column(Integer, primary_key=True, autoincrement=True)
    intent = Column(String(50), index=True)
    crop_id = Column(String(50), index=True, nullable=True)
    language = Column(String(20))
    question = Column(Text)
    answer_en = Column(Text)
    answer_hi = Column(Text)
    source = Column(String(200))
    updated_at = Column(DateTime, default=datetime.datetime.utcnow)

class MarketPrice(Base):
    __tablename__ = "market_prices"

    id = Column(Integer, primary_key=True, autoincrement=True)
    crop_id = Column(String(50), index=True)
    commodity = Column(String(100), index=True, nullable=False)
    variety = Column(String(100), default="Standard")
    state = Column(String(100), index=True, nullable=False)
    district = Column(String(100), index=True, nullable=False)
    market = Column(String(100), index=True, nullable=False)
    min_price = Column(Float, nullable=False)
    max_price = Column(Float, nullable=False)
    modal_price = Column(Float, nullable=False)
    price_date = Column(String(50), index=True, nullable=False)
    unit = Column(String(50), default="₹/Quintal")
    source = Column(String(200), default="DAFW / Agmarknet / Kisan Call Centre")
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

class CropVariety(Base):
    __tablename__ = "crop_varieties"

    id = Column(String(100), primary_key=True, index=True)
    crop_id = Column(String(50), index=True, nullable=False)
    variety_name = Column(String(100), nullable=False)
    category = Column(String(100))
    duration_days = Column(String(100))
    yield_potential = Column(String(100))
    suitable_zones = Column(Text)
    special_features = Column(Text)
    special_features_hi = Column(Text)
    source = Column(String(200))
    updated_at = Column(DateTime, default=datetime.datetime.utcnow)

