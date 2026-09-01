from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

class BookingRequest(BaseModel):
    equipment_id: str
    farmer_name: str
    phone: str
    hours: int

_EQUIPMENT = [
    {"id": "EQ-01", "name": "Mahindra 575 DI (45 HP Tractor)", "owner": "रामेश्वर सिंह (Rameshwar)", "rate_per_hour": 600, "distance_km": 2.4, "available": True, "type": "Tractor"},
    {"id": "EQ-02", "name": "Rotavator (7 Feet Shaktiman)", "owner": "सुरेश कुमार (Suresh)", "rate_per_hour": 400, "distance_km": 3.1, "available": True, "type": "Tillage"},
    {"id": "EQ-03", "name": "Combine Harvester (Kartar)", "owner": "बलदेव चौधरी (Baldev)", "rate_per_hour": 1800, "distance_km": 5.8, "available": True, "type": "Harvester"},
    {"id": "EQ-04", "name": "Laser Land Leveler", "owner": "अजय पाल (Ajay Pal)", "rate_per_hour": 750, "distance_km": 4.2, "available": True, "type": "Leveler"},
    {"id": "EQ-05", "name": "Zero-Till Seed Drill", "owner": "विनोद त्यागी (Vinod)", "rate_per_hour": 500, "distance_km": 1.9, "available": True, "type": "Seeder"}
]

@router.get("")
def list_equipment():
    return _EQUIPMENT

@router.post("/book")
def book_equipment(req: BookingRequest):
    return {
        "status": "confirmed",
        "booking_id": f"BK-{req.equipment_id}-992",
        "message": f"✓ {req.farmer_name} जी, आपकी बुकिंग दर्ज कर ली गई है! उपकरण मालिक आपसे {req.phone} पर संपर्क करेंगे।"
    }
