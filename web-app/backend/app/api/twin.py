from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import time

router = APIRouter()

class GeoPoint(BaseModel):
    latitude: float
    longitude: float

class CreateFieldRequest(BaseModel):
    name: str
    crop_name: str
    crop_name_hi: str
    area_acres: float
    boundary: List[GeoPoint]

class IrrigationActionRequest(BaseModel):
    zone_id: str
    amount_mm: Optional[float] = 35.0
    notes: Optional[str] = None

# In-memory Twin State
_FIELDS = {
    "FIELD-001": {
        "id": "FIELD-001",
        "name": "My Wheat Field",
        "crop_name": "Wheat",
        "crop_name_hi": "गेहूं (Wheat)",
        "area_acres": 5.2,
        "sowing_date": "15 Nov 2025",
        "boundary": [
            {"latitude": 28.7040, "longitude": 77.1000},
            {"latitude": 28.7055, "longitude": 77.1045},
            {"latitude": 28.7015, "longitude": 77.1060},
            {"latitude": 28.7000, "longitude": 77.1015}
        ],
        "center": {"latitude": 28.70275, "longitude": 77.1030}
    }
}

_ZONES: Dict[str, Dict[str, Any]] = {}
_LAST_ACTION = None

def _reset_demo_zones():
    global _ZONES
    _ZONES.clear()
    now = int(time.time())
    
    # 9-zone regular grid coordinates derived from the demo boundary
    base_lat = 28.7000
    base_lon = 77.1000
    d_lat = 0.0018
    d_lon = 0.0020
    
    for i in range(1, 10):
        row = (i - 1) // 3
        col = (i - 1) % 3
        zid = f"Z-{i:02d}"
        
        z_min_lat = base_lat + row * d_lat
        z_max_lat = z_min_lat + d_lat
        z_min_lon = base_lon + col * d_lon
        z_max_lon = z_min_lon + d_lon
        
        boundary = [
            {"latitude": z_min_lat, "longitude": z_min_lon},
            {"latitude": z_max_lat, "longitude": z_min_lon},
            {"latitude": z_max_lat, "longitude": z_max_lon},
            {"latitude": z_min_lat, "longitude": z_max_lon}
        ]
        
        # Zone 7: Acute Water Stress
        if zid == "Z-07":
            _ZONES[zid] = {
                "id": zid,
                "label": f"Zone {i}",
                "label_hi": f"ज़ोन {i}",
                "area_acres": 0.58,
                "moisture": 18,
                "ndvi": 0.49,
                "temperature": 36.0,
                "rainfall_mm": 2.0,
                "humidity": 42,
                "crop_health": "POOR",
                "risk_level": "HIGH",
                "risk_type": "WATER_STRESS",
                "boundary": boundary,
                "center": {"latitude": (z_min_lat + z_max_lat)/2, "longitude": (z_min_lon + z_max_lon)/2},
                "history": [
                    {"date": "01 Aug", "moisture": 32, "ndvi": 0.68},
                    {"date": "10 Aug", "moisture": 28, "ndvi": 0.62},
                    {"date": "20 Aug", "moisture": 22, "ndvi": 0.55},
                    {"date": "Today", "moisture": 18, "ndvi": 0.49}
                ]
            }
        elif zid == "Z-05":
            _ZONES[zid] = {
                "id": zid,
                "label": f"Zone {i}",
                "label_hi": f"ज़ोन {i}",
                "area_acres": 0.58,
                "moisture": 21,
                "ndvi": 0.54,
                "temperature": 34.0,
                "rainfall_mm": 5.0,
                "humidity": 50,
                "crop_health": "MODERATE",
                "risk_level": "MEDIUM",
                "risk_type": "WATER_STRESS",
                "boundary": boundary,
                "center": {"latitude": (z_min_lat + z_max_lat)/2, "longitude": (z_min_lon + z_max_lon)/2},
                "history": [
                    {"date": "10 Aug", "moisture": 26, "ndvi": 0.60},
                    {"date": "Today", "moisture": 21, "ndvi": 0.54}
                ]
            }
        else:
            _ZONES[zid] = {
                "id": zid,
                "label": f"Zone {i}",
                "label_hi": f"ज़ोन {i}",
                "area_acres": 0.58,
                "moisture": 30,
                "ndvi": 0.70,
                "temperature": 31.5,
                "rainfall_mm": 14.0,
                "humidity": 65,
                "crop_health": "GOOD",
                "risk_level": "LOW",
                "risk_type": "NONE",
                "boundary": boundary,
                "center": {"latitude": (z_min_lat + z_max_lat)/2, "longitude": (z_min_lon + z_max_lon)/2},
                "history": [
                    {"date": "Today", "moisture": 30, "ndvi": 0.70}
                ]
            }

_reset_demo_zones()

@router.get("")
def get_field_twin():
    """Retrieve full Field Digital Twin state"""
    field = _FIELDS.get("FIELD-001")
    avg_moisture = int(sum(z["moisture"] for z in _ZONES.values()) / len(_ZONES))
    avg_ndvi = round(sum(z["ndvi"] for z in _ZONES.values()) / len(_ZONES), 2)
    attention_count = sum(1 for z in _ZONES.values() if z["risk_level"] in ("HIGH", "MEDIUM"))
    
    return {
        "field": field,
        "zones": list(_ZONES.values()),
        "summary": {
            "avg_moisture": avg_moisture,
            "avg_ndvi": avg_ndvi,
            "attention_zones_count": attention_count,
            "data_source": "Demonstration Data (SIH Prototype)",
            "last_action": _LAST_ACTION
        }
    }

@router.post("/irrigation")
def record_irrigation(req: IrrigationActionRequest):
    """
    Closed-loop Action Feedback:
    Farmer records irrigation -> Twin state updates dynamically!
    """
    global _LAST_ACTION
    zid = req.zone_id
    if zid not in _ZONES:
        raise HTTPException(status_code=404, detail="Zone not found")
        
    zone = _ZONES[zid]
    # Update state: Moisture restored from 18% to 32%
    zone["moisture"] = 32
    zone["ndvi"] = 0.58
    zone["crop_health"] = "GOOD"
    zone["risk_level"] = "LOW"
    zone["risk_type"] = "NONE"
    
    zone["history"].append({
        "date": "Today (Post-Irrigation)",
        "moisture": 32,
        "ndvi": 0.58
    })
    
    _LAST_ACTION = f"Zone {zid}: Irrigated {req.amount_mm}mm today — moisture restored to 32%"
    
    return {
        "status": "success",
        "updated_zone": zone,
        "message": f"✓ {zone['label_hi']} में {req.amount_mm} मिमी सिंचाई दर्ज की गई। डिजिटल ट्विन अपडेट हुआ।"
    }

@router.post("/reset")
def reset_demo():
    """Reset demo scenario to initial water stress state for presentations"""
    _reset_demo_zones()
    return {"status": "reset", "message": "Demo state reset to initial water stress"}
