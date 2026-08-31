from typing import List, Optional, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import desc
from backend.app.db.models import MarketPrice, CropVariety

class MarketService:
    @staticmethod
    def get_prices(
        db: Session,
        crop_id: Optional[str] = None,
        commodity: Optional[str] = None,
        state: Optional[str] = None,
        district: Optional[str] = None,
        market: Optional[str] = None,
        limit: int = 50
    ) -> List[MarketPrice]:
        query = db.query(MarketPrice)
        if crop_id:
            query = query.filter(MarketPrice.crop_id == crop_id)
        if commodity:
            query = query.filter(MarketPrice.commodity.ilike(f"%{commodity}%"))
        if state:
            query = query.filter(MarketPrice.state.ilike(f"%{state}%"))
        if district:
            query = query.filter(MarketPrice.district.ilike(f"%{district}%"))
        if market:
            query = query.filter(MarketPrice.market.ilike(f"%{market}%"))
        return query.order_by(desc(MarketPrice.price_date), desc(MarketPrice.id)).limit(limit).all()

    @staticmethod
    def get_latest_price(
        db: Session,
        crop_id: Optional[str] = None,
        commodity: Optional[str] = None,
        district: Optional[str] = None,
        market: Optional[str] = None,
        state: Optional[str] = None
    ) -> Optional[MarketPrice]:
        query = db.query(MarketPrice)
        if crop_id:
            query = query.filter(MarketPrice.crop_id == crop_id)
        if commodity:
            query = query.filter(MarketPrice.commodity.ilike(f"%{commodity}%"))
        if market:
            query = query.filter(MarketPrice.market.ilike(f"%{market}%"))
        if district:
            query = query.filter(MarketPrice.district.ilike(f"%{district}%"))
        if state:
            query = query.filter(MarketPrice.state.ilike(f"%{state}%"))
        return query.order_by(desc(MarketPrice.price_date), desc(MarketPrice.id)).first()

    @staticmethod
    def compare_markets(
        db: Session,
        crop_id: Optional[str] = None,
        commodity: Optional[str] = None,
        state: Optional[str] = None
    ) -> Dict[str, Any]:
        query = db.query(MarketPrice)
        if crop_id:
            query = query.filter(MarketPrice.crop_id == crop_id)
        if commodity:
            query = query.filter(MarketPrice.commodity.ilike(f"%{commodity}%"))
        if state:
            query = query.filter(MarketPrice.state.ilike(f"%{state}%"))

        records = query.order_by(desc(MarketPrice.modal_price)).limit(20).all()
        if not records:
            return None

        items = []
        total_modal = 0.0
        for r in records:
            items.append({
                "market": r.market,
                "district": r.district,
                "state": r.state,
                "modal_price": r.modal_price,
                "min_price": r.min_price,
                "max_price": r.max_price,
                "price_date": r.price_date,
                "unit": r.unit
            })
            total_modal += r.modal_price

        sorted_by_price = sorted(items, key=lambda x: x["modal_price"], reverse=True)
        return {
            "commodity": records[0].commodity,
            "crop_id": records[0].crop_id,
            "best_market": sorted_by_price[0] if sorted_by_price else None,
            "cheapest_market": sorted_by_price[-1] if sorted_by_price else None,
            "markets": items,
            "average_modal_price": round(total_modal / len(items), 2) if items else 0.0,
            "price_date": records[0].price_date,
            "source": records[0].source
        }

    @staticmethod
    def get_varieties(
        db: Session,
        crop_id: Optional[str] = None,
        category: Optional[str] = None
    ) -> List[CropVariety]:
        query = db.query(CropVariety)
        if crop_id:
            query = query.filter(CropVariety.crop_id == crop_id)
        if category:
            query = query.filter(CropVariety.category.ilike(f"%{category}%"))
        return query.all()
