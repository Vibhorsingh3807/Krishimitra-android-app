from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from backend.app.db.session import get_db
from backend.app.schemas.market import (
    MarketPriceResponse,
    MarketComparisonResponse,
    CropVarietyResponse
)
from backend.app.services.market_service import MarketService

router = APIRouter()

@router.get("", response_model=List[MarketPriceResponse])
def get_market_prices(
    crop_id: Optional[str] = None,
    commodity: Optional[str] = None,
    state: Optional[str] = None,
    district: Optional[str] = None,
    market: Optional[str] = None,
    limit: int = Query(50, le=200),
    db: Session = Depends(get_db)
):
    return MarketService.get_prices(
        db=db,
        crop_id=crop_id,
        commodity=commodity,
        state=state,
        district=district,
        market=market,
        limit=limit
    )

@router.get("/latest", response_model=MarketPriceResponse)
def get_latest_price(
    crop_id: Optional[str] = None,
    commodity: Optional[str] = None,
    district: Optional[str] = None,
    market: Optional[str] = None,
    state: Optional[str] = None,
    db: Session = Depends(get_db)
):
    price = MarketService.get_latest_price(
        db=db,
        crop_id=crop_id,
        commodity=commodity,
        district=district,
        market=market,
        state=state
    )
    if not price:
        raise HTTPException(status_code=404, detail="No price record found for the requested commodity/market")
    return price

@router.get("/compare", response_model=MarketComparisonResponse)
def compare_markets(
    crop_id: Optional[str] = None,
    commodity: Optional[str] = None,
    state: Optional[str] = None,
    db: Session = Depends(get_db)
):
    result = MarketService.compare_markets(db=db, crop_id=crop_id, commodity=commodity, state=state)
    if not result:
        raise HTTPException(status_code=404, detail="No market comparison data found")
    return result

@router.get("/varieties", response_model=List[CropVarietyResponse])
def get_crop_varieties(
    crop_id: Optional[str] = None,
    category: Optional[str] = None,
    db: Session = Depends(get_db)
):
    return MarketService.get_varieties(db=db, crop_id=crop_id, category=category)
