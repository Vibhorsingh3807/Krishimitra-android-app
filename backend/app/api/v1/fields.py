from typing import List, Optional, Dict, Any
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field
import time

router = APIRouter()

# --- Pydantic Schemas ---
class GeoPointModel(BaseModel):
    latitude: float
    longitude: float

class FieldCreateRequest(BaseModel):
    name: str
    crop_id: str
    crop_name: str
    crop_name_hi: str
    variety: Optional[str] = None
    area_acres: float
    sowing_date: str
    boundary: List[GeoPointModel]

class ZoneObservationModel(BaseModel):
    id: str
    zone_id: str
    timestamp: int
    date_string: str
    ndvi: float
    moisture: int
    temperature: float
    rainfall_mm: float
    humidity: int
    crop_health: str
    risk_level: str
    risk_type: str
    risk_type_hi: str
    source: str = "Demonstration Data"

class IrrigationEventRequest(BaseModel):
    amount_mm: Optional[float] = 35.0
    notes: Optional[str] = None

class InspectionEventRequest(BaseModel):
    notes: Optional[str] = None
    crop_health_observed: Optional[str] = "Good"

# --- In-Memory Repository for SIH Prototype Demonstration ---
_DEMO_BOUNDARY = [
    {"latitude": 28.7040, "longitude": 77.1000},
    {"latitude": 28.7055, "longitude": 77.1045},
    {"latitude": 28.7015, "longitude": 77.1060},
    {"latitude": 28.7000, "longitude": 77.1015}
]

_FIELDS_DB: Dict[str, Dict[str, Any]] = {
    "FIELD-001": {
        "id": "FIELD-001",
        "name": "My Wheat Field",
        "crop_id": "wheat",
        "crop_name": "Wheat",
        "crop_name_hi": "गेहूं (Wheat)",
        "variety": "HD-2967 (पूसा)",
        "area_acres": 5.2,
        "sowing_date": "15 Nov 2025",
        "boundary": _DEMO_BOUNDARY,
        "created_at": int(time.time()) - 45 * 86400
    }
}

_ZONES_DB: Dict[str, List[Dict[str, Any]]] = {}
_OBSERVATIONS_DB: Dict[str, List[Dict[str, Any]]] = {}
_IRRIGATION_DB: Dict[str, List[Dict[str, Any]]] = {}

def _init_demo_zones(field_id: str):
    zones = []
    now = int(time.time())
    for i in range(1, 10):
        zid = f"Z-{i:02d}"
        zones.append({
            "id": zid,
            "field_id": field_id,
            "zone_number": i,
            "zone_label": f"Zone {i}",
            "zone_label_hi": f"ज़ोन {i}",
            "area_acres": 0.58,
            "crop": "Wheat",
            "crop_hi": "गेहूं"
        })
        # Initial deterministic state
        if zid == "Z-07":
            # Acute water stress zone
            _OBSERVATIONS_DB[zid] = [
                {"id": f"OBS-{zid}-1", "zone_id": zid, "timestamp": now - 15*86400, "date_string": "01 Aug", "ndvi": 0.68, "moisture": 32, "temperature": 29.5, "rainfall_mm": 25, "humidity": 72, "crop_health": "Good", "risk_level": "LOW", "risk_type": "NONE", "risk_type_hi": "सामान्य", "source": "Demonstration Data"},
                {"id": f"OBS-{zid}-2", "zone_id": zid, "timestamp": now - 10*86400, "date_string": "10 Aug", "ndvi": 0.62, "moisture": 28, "temperature": 31.0, "rainfall_mm": 18, "humidity": 65, "crop_health": "Good", "risk_level": "LOW", "risk_type": "NONE", "risk_type_hi": "सामान्य", "source": "Demonstration Data"},
                {"id": f"OBS-{zid}-3", "zone_id": zid, "timestamp": now - 5*86400, "date_string": "20 Aug", "ndvi": 0.55, "moisture": 22, "temperature": 33.8, "rainfall_mm": 5, "humidity": 54, "crop_health": "Moderate", "risk_level": "MEDIUM", "risk_type": "WATER_STRESS", "risk_type_hi": "नमी में गिरावट", "source": "Demonstration Data"},
                {"id": f"OBS-{zid}-4", "zone_id": zid, "timestamp": now, "date_string": "आज (Today)", "ndvi": 0.49, "moisture": 18, "temperature": 36.0, "rainfall_mm": 2, "humidity": 42, "crop_health": "Poor", "risk_level": "HIGH", "risk_type": "WATER_STRESS", "risk_type_hi": "गंभीर पानी की कमी (Water Stress)", "source": "Demonstration Data"}
            ]
        elif zid == "Z-05":
            _OBSERVATIONS_DB[zid] = [
                {"id": f"OBS-{zid}-1", "zone_id": zid, "timestamp": now - 10*86400, "date_string": "10 Aug", "ndvi": 0.60, "moisture": 26, "temperature": 32.0, "rainfall_mm": 12, "humidity": 60, "crop_health": "Moderate", "risk_level": "LOW", "risk_type": "NONE", "risk_type_hi": "सामान्य", "source": "Demonstration Data"},
                {"id": f"OBS-{zid}-2", "zone_id": zid, "timestamp": now, "date_string": "आज (Today)", "ndvi": 0.54, "moisture": 21, "temperature": 34.2, "rainfall_mm": 5, "humidity": 52, "crop_health": "Moderate", "risk_level": "MEDIUM", "risk_type": "WATER_STRESS", "risk_type_hi": "मध्यम तनाव", "source": "Demonstration Data"}
            ]
        else:
            _OBSERVATIONS_DB[zid] = [
                {"id": f"OBS-{zid}-1", "zone_id": zid, "timestamp": now, "date_string": "आज (Today)", "ndvi": 0.70, "moisture": 30, "temperature": 31.5, "rainfall_mm": 14, "humidity": 65, "crop_health": "Excellent", "risk_level": "LOW", "risk_type": "NONE", "risk_type_hi": "सामान्य", "source": "Demonstration Data"}
            ]
    _ZONES_DB[field_id] = zones

_init_demo_zones("FIELD-001")

# --- REST Endpoints ---

@router.get("")
def list_fields():
    """List all farmer fields"""
    return list(_FIELDS_DB.values())

@router.post("")
def create_field(req: FieldCreateRequest):
    """Create a new farmer field and trigger 9-zone generation"""
    fid = f"FIELD-{len(_FIELDS_DB) + 1:03d}"
    field_data = {
        "id": fid,
        "name": req.name,
        "crop_id": req.crop_id,
        "crop_name": req.crop_name,
        "crop_name_hi": req.crop_name_hi,
        "variety": req.variety,
        "area_acres": req.area_acres,
        "sowing_date": req.sowing_date,
        "boundary": [p.dict() for p in req.boundary],
        "created_at": int(time.time())
    }
    _FIELDS_DB[fid] = field_data
    _init_demo_zones(fid)
    return field_data

@router.get("/{field_id}")
def get_field(field_id: str):
    """Retrieve field by ID"""
    if field_id not in _FIELDS_DB:
        raise HTTPException(status_code=404, detail="Field not found")
    return _FIELDS_DB[field_id]

@router.post("/{field_id}/zones/generate")
def generate_zones(field_id: str):
    """Regenerate management zones for a field"""
    if field_id not in _FIELDS_DB:
        raise HTTPException(status_code=404, detail="Field not found")
    _init_demo_zones(field_id)
    return {"status": "success", "zones": _ZONES_DB[field_id]}

@router.get("/{field_id}/twin")
def get_field_twin_summary(field_id: str):
    """Retrieve complete living Digital Twin state of the field"""
    if field_id not in _FIELDS_DB:
        raise HTTPException(status_code=404, detail="Field not found")
    
    field = _FIELDS_DB[field_id]
    zones = _ZONES_DB.get(field_id, [])
    
    latest_obs = {}
    for z in zones:
        obs_list = _OBSERVATIONS_DB.get(z["id"], [])
        if obs_list:
            latest_obs[z["id"]] = obs_list[-1]
            
    return {
        "field": field,
        "zones": zones,
        "latest_observations": latest_obs,
        "data_source": "Demonstration Data (SIH Prototype)",
        "last_updated": int(time.time())
    }

@router.get("/zones/{zone_id}")
def get_zone(zone_id: str):
    """Get zone metadata"""
    for flist in _ZONES_DB.values():
        for z in flist:
            if z["id"] == zone_id:
                return z
    raise HTTPException(status_code=404, detail="Zone not found")

@router.get("/zones/{zone_id}/observations")
def get_zone_observations(zone_id: str):
    """Get time-series observations for a zone"""
    return _OBSERVATIONS_DB.get(zone_id, [])

@router.post("/zones/{zone_id}/observations")
def add_zone_observation(zone_id: str, obs: ZoneObservationModel):
    """Record a new observation (e.g. from Camera AI, Drone, or Sensor)"""
    if zone_id not in _OBSERVATIONS_DB:
        _OBSERVATIONS_DB[zone_id] = []
    _OBSERVATIONS_DB[zone_id].append(obs.dict())
    return {"status": "recorded", "observation": obs}

@router.post("/zones/{zone_id}/irrigation")
def record_irrigation_action(zone_id: str, req: IrrigationEventRequest):
    """Record farmer irrigation action and trigger closed-loop twin state update"""
    now = int(time.time())
    event = {
        "id": f"IRR-{now}",
        "zone_id": zone_id,
        "timestamp": now,
        "amount_mm": req.amount_mm,
        "notes": req.notes or "35mm irrigation delivered to canopy and root zone",
        "source": "Farmer Action"
    }
    if zone_id not in _IRRIGATION_DB:
        _IRRIGATION_DB[zone_id] = []
    _IRRIGATION_DB[zone_id].insert(0, event)
    
    # Closed-loop Twin update: Zone state dynamically transitions to healthy
    updated_obs = {
        "id": f"OBS-UPDATED-{now}",
        "zone_id": zone_id,
        "timestamp": now,
        "date_string": "आज (सिंचाई उपरांत)",
        "ndvi": 0.58,
        "moisture": 32, # Restored
        "temperature": 31.0,
        "rainfall_mm": 2.0,
        "humidity": 65,
        "crop_health": "Good",
        "risk_level": "LOW",
        "risk_type": "NONE",
        "risk_type_hi": "सामान्य (सिंचाई उपरांत सुधार)",
        "source": "Farmer Action + Twin Simulation"
    }
    if zone_id not in _OBSERVATIONS_DB:
        _OBSERVATIONS_DB[zone_id] = []
    _OBSERVATIONS_DB[zone_id].append(updated_obs)
    
    return {
        "status": "success",
        "action_recorded": event,
        "updated_twin_state": updated_obs,
        "message": f"Zone {zone_id} soil moisture restored to 32%. Digital Twin updated."
    }

@router.post("/zones/{zone_id}/inspection")
def record_inspection_action(zone_id: str, req: InspectionEventRequest):
    """Log manual field scout inspection"""
    return {
        "status": "logged",
        "zone_id": zone_id,
        "notes": req.notes,
        "observed_health": req.crop_health_observed,
        "timestamp": int(time.time())
    }

@router.get("/zones/{zone_id}/recommendations")
def get_zone_recommendations(zone_id: str):
    """Get rule-based agronomic recommendations for a zone"""
    obs_list = _OBSERVATIONS_DB.get(zone_id, [])
    if not obs_list:
        return []
    latest = obs_list[-1]
    
    if latest["moisture"] < 20:
        return [{
            "id": f"REC-{zone_id}-01",
            "zone_id": zone_id,
            "title": "Water Stress Alert: Immediate Irrigation Needed",
            "title_hi": "पानी की कमी की चेतावनी: तत्काल सिंचाई आवश्यक",
            "priority": "HIGH",
            "recommendation": f"Zone {zone_id} moisture has fallen to {latest['moisture']}%. Immediate 35mm irrigation recommended.",
            "recommendation_hi": f"{zone_id} में नमी घटकर {latest['moisture']}% रह गई है। तुरंत 35 मिमी सिंचाई करें।"
        }]
    return []
