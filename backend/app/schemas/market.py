from typing import List, Optional
from pydantic import BaseModel, ConfigDict

class MarketPriceBase(BaseModel):
    crop_id: Optional[str] = None
    commodity: str
    variety: Optional[str] = "Standard"
    state: str
    district: str
    market: str
    min_price: float
    max_price: float
    modal_price: float
    price_date: str
    unit: str = "₹/Quintal"
    source: str = "DAFW / Agmarknet / Kisan Call Centre"

class MarketPriceResponse(MarketPriceBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

class MarketComparisonItem(BaseModel):
    market: str
    district: str
    state: str
    modal_price: float
    min_price: float
    max_price: float
    price_date: str
    unit: str

class MarketComparisonResponse(BaseModel):
    commodity: str
    crop_id: Optional[str] = None
    best_market: Optional[MarketComparisonItem] = None
    cheapest_market: Optional[MarketComparisonItem] = None
    markets: List[MarketComparisonItem]
    average_modal_price: float
    price_date: str
    source: str

class CropVarietyResponse(BaseModel):
    id: str
    crop_id: str
    variety_name: str
    category: Optional[str] = None
    duration_days: Optional[str] = None
    yield_potential: Optional[str] = None
    suitable_zones: Optional[str] = None
    special_features: Optional[str] = None
    special_features_hi: Optional[str] = None
    source: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)
