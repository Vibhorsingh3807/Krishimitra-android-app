from fastapi import APIRouter
from typing import List, Dict, Any

router = APIRouter()

_CROPS = [
    {"id": "wheat", "name": "Wheat", "name_hi": "गेहूं", "season": "Rabi", "water": "Medium (4-6 Irrigations)", "yield": "45-55 Quintals/Ha", "soil": "Alluvial, Loamy", "duration": "120-140 Days"},
    {"id": "rice", "name": "Rice / Paddy", "name_hi": "धान / चावल", "season": "Kharif", "water": "High (Standing Water)", "yield": "50-60 Quintals/Ha", "soil": "Clayey, Clay Loam", "duration": "110-140 Days"},
    {"id": "mustard", "name": "Mustard", "name_hi": "सरसों", "season": "Rabi", "water": "Low (2-3 Irrigations)", "yield": "18-22 Quintals/Ha", "soil": "Sandy Loam to Clay", "duration": "105-125 Days"},
    {"id": "sugarcane", "name": "Sugarcane", "name_hi": "गन्ना", "season": "Annual", "water": "Very High", "yield": "750-850 Quintals/Ha", "soil": "Deep Well-drained Loam", "duration": "300-360 Days"},
    {"id": "cotton", "name": "Cotton", "name_hi": "कपास", "season": "Kharif", "water": "Medium", "yield": "20-25 Quintals/Ha", "soil": "Black Cotton Soil", "duration": "150-180 Days"},
    {"id": "potato", "name": "Potato", "name_hi": "आलू", "season": "Rabi", "water": "Medium to High", "yield": "250-350 Quintals/Ha", "soil": "Sandy Loam", "duration": "90-110 Days"},
    {"id": "tomato", "name": "Tomato", "name_hi": "टमाटर", "season": "Rabi / Kharif", "water": "Frequent Light", "yield": "300-400 Quintals/Ha", "soil": "Well-drained Sandy Loam", "duration": "100-120 Days"},
    {"id": "chickpea", "name": "Chickpea / Gram", "name_hi": "चना", "season": "Rabi", "water": "Low (1-2 Irrigations)", "yield": "18-22 Quintals/Ha", "soil": "Sandy Loam to Clay", "duration": "95-115 Days"},
    {"id": "maize", "name": "Maize / Corn", "name_hi": "मक्का", "season": "Kharif / Spring", "water": "Medium", "yield": "45-55 Quintals/Ha", "soil": "Well-drained Loam", "duration": "90-105 Days"},
    {"id": "soybean", "name": "Soybean", "name_hi": "सोयाबीन", "season": "Kharif", "water": "Medium", "yield": "20-25 Quintals/Ha", "soil": "Well-drained Loam", "duration": "90-100 Days"},
    {"id": "onion", "name": "Onion", "name_hi": "प्याज", "season": "Rabi / Kharif", "water": "Frequent", "yield": "250-300 Quintals/Ha", "soil": "Rich Sandy Loam", "duration": "120-130 Days"},
    {"id": "garlic", "name": "Garlic", "name_hi": "लहसुन", "season": "Rabi", "water": "Medium", "yield": "80-100 Quintals/Ha", "soil": "Fertile Loamy Soil", "duration": "130-150 Days"}
]

@router.get("")
def list_crops():
    """List ICAR crop cultivation manuals"""
    return _CROPS
