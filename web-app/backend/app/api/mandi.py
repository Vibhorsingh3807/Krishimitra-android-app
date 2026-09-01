from fastapi import APIRouter

router = APIRouter()

_MANDI_PRICES = [
    {"commodity": "Wheat (गेहूं)", "mandi": "Hapur Mandi (UP)", "modal_price": 2420, "msp": 2275, "change": "+₹45", "trend": "up"},
    {"commodity": "Paddy / Basmati (धान)", "mandi": "Karnal Mandi (HR)", "modal_price": 3650, "msp": 2203, "change": "+₹110", "trend": "up"},
    {"commodity": "Mustard (सरसों)", "mandi": "Alwar Mandi (RJ)", "modal_price": 5480, "msp": 5650, "change": "-₹30", "trend": "down"},
    {"commodity": "Potato (आलू)", "mandi": "Agra Mandi (UP)", "modal_price": 1450, "msp": 0, "change": "+₹60", "trend": "up"},
    {"commodity": "Cotton (कपास)", "mandi": "Rajkot Mandi (GJ)", "modal_price": 7120, "msp": 6620, "change": "+₹85", "trend": "up"},
    {"commodity": "Tomato (टमाटर)", "mandi": "Azadpur Mandi (DL)", "modal_price": 1850, "msp": 0, "change": "-₹90", "trend": "down"}
]

@router.get("")
def get_mandi_prices():
    return _MANDI_PRICES
