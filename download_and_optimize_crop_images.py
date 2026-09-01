import os
import json
import urllib.request
import urllib.parse
from PIL import Image, ImageDraw, ImageFont
import io

OUTPUT_DIR = os.path.join("android", "app", "src", "main", "assets", "crops")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 49 Crops with targeted Wikipedia image query and fallback color palette
CROP_QUERIES = {
    "arhar": ("Pigeon_pea", (180, 130, 70), "Arhar / Tur", "🌾"),
    "bajra": ("Pearl_millet", (160, 150, 90), "Bajra", "🌾"),
    "banana": ("Banana", (220, 200, 50), "Banana", "🍌"),
    "black_pepper": ("Black_pepper", (50, 70, 50), "Black Pepper", "🌿"),
    "brinjal": ("Eggplant", (100, 50, 120), "Brinjal", "🍆"),
    "cabbage": ("Cabbage", (90, 160, 90), "Cabbage", "🥬"),
    "carrot": ("Carrot", (230, 110, 30), "Carrot", "🥕"),
    "cauliflower": ("Cauliflower", (210, 210, 190), "Cauliflower", "🥦"),
    "chickpea": ("Chickpea", (200, 160, 100), "Chickpea / Chana", "🌱"),
    "chilli": ("Chili_pepper", (200, 40, 40), "Chilli", "🌶️"),
    "citrus": ("Citrus", (240, 150, 30), "Citrus / Orange", "🍊"),
    "coffee": ("Coffea", (100, 60, 40), "Coffee", "☕"),
    "coriander": ("Coriander", (70, 140, 70), "Coriander", "🌿"),
    "cotton": ("Gossypium", (220, 220, 220), "Cotton", "☁️"),
    "cumin": ("Cuminum_cyminum", (160, 130, 90), "Cumin / Jeera", "🌿"),
    "garlic": ("Garlic", (220, 220, 210), "Garlic", "🧄"),
    "ginger": ("Ginger", (190, 160, 110), "Ginger", "🫚"),
    "grapes": ("Grape", (120, 50, 120), "Grapes", "🍇"),
    "green_peas": ("Pea", (80, 170, 70), "Green Peas", "🫛"),
    "groundnut": ("Peanut", (190, 140, 80), "Groundnut", "🥜"),
    "guava": ("Guava", (120, 180, 70), "Guava", "🍈"),
    "jowar": ("Sorghum_bicolor", (170, 140, 80), "Jowar", "🌾"),
    "jute": ("Jute", (150, 160, 90), "Jute", "🌱"),
    "maize": ("Maize", (230, 180, 40), "Maize / Corn", "🌽"),
    "mango": ("Mango", (240, 160, 40), "Mango", "🥭"),
    "masoor": ("Lentil", (200, 100, 60), "Masoor", "🌱"),
    "moong": ("Mung_bean", (100, 150, 80), "Moong", "🌱"),
    "mustard": ("Brassica_juncea", (230, 200, 40), "Mustard", "🌼"),
    "okra": ("Okra", (70, 150, 60), "Okra / Bhindi", "🌿"),
    "onion": ("Onion", (180, 80, 90), "Onion", "🧅"),
    "papaya": ("Papaya", (240, 140, 30), "Papaya", "🍈"),
    "peas": ("Field_pea", (90, 160, 80), "Field Pea", "🫛"),
    "pomegranate": ("Pomegranate", (180, 40, 50), "Pomegranate", "🍎"),
    "potato": ("Potato", (170, 130, 80), "Potato", "🥔"),
    "radish": ("Radish", (220, 220, 230), "Radish", "🌱"),
    "ragi": ("Eleusine_coracana", (140, 90, 60), "Ragi", "🌾"),
    "rice": ("Rice", (120, 170, 80), "Rice / Paddy", "🌾"),
    "rubber": ("Natural_rubber", (80, 120, 80), "Rubber", "🌳"),
    "safflower": ("Carthamus_tinctorius", (230, 140, 30), "Safflower", "🌼"),
    "sesame": ("Sesame", (210, 190, 150), "Sesame / Til", "🌱"),
    "soybean": ("Soybean", (130, 160, 80), "Soybean", "🌱"),
    "sugarcane": ("Sugarcane", (110, 160, 70), "Sugarcane", "🎋"),
    "sunflower": ("Helianthus_annuus", (240, 190, 30), "Sunflower", "🌻"),
    "tea": ("Camellia_sinensis", (50, 110, 50), "Tea", "🍵"),
    "tomato": ("Tomato", (220, 50, 40), "Tomato", "🍅"),
    "turmeric": ("Turmeric", (230, 150, 20), "Turmeric", "🌱"),
    "urad": ("Vigna_mungo", (60, 70, 60), "Urad", "🌱"),
    "watermelon": ("Watermelon", (50, 140, 70), "Watermelon", "🍉"),
    "wheat": ("Wheat", (220, 180, 90), "Wheat", "🌾")
}

def generate_fallback_image(crop_id, color, name, emoji):
    """Generate a clean, high-contrast botanical placeholder thumbnail"""
    img = Image.new("RGB", (320, 220), color)
    draw = ImageDraw.Draw(img)
    
    # Draw decorative field/gradient lines
    draw.rectangle([0, 160, 320, 220], fill=(int(color[0]*0.75), int(color[1]*0.75), int(color[2]*0.75)))
    draw.rectangle([0, 0, 320, 160], fill=color)
    
    # Draw simple badge circle
    cx, cy, r = 160, 85, 45
    draw.ellipse([cx-r, cy-r, cx+r, cy+r], fill=(255, 255, 255, 200), outline=(255, 255, 255), width=3)
    
    # Label text at bottom
    # Default font
    draw.text((160, 185), name, fill=(255, 255, 255), anchor="mm")
    
    out_path = os.path.join(OUTPUT_DIR, f"{crop_id}.webp")
    img.save(out_path, "WEBP", quality=80)
    return out_path

def fetch_and_save_crop_image(crop_id, wiki_title, fallback_color, name, emoji):
    out_path = os.path.join(OUTPUT_DIR, f"{crop_id}.webp")
    if os.path.exists(out_path) and os.path.getsize(out_path) > 1000:
        return out_path

    try:
        url = f"https://en.wikipedia.org/w/api.php?action=query&titles={wiki_title}&prop=pageimages&format=json&pithumbsize=400"
        req = urllib.request.Request(url, headers={"User-Agent": "KrishiMitraAgriApp/1.0 (agri_app@krishimitra.org)"})
        with urllib.request.urlopen(req, timeout=6) as response:
            data = json.loads(response.read().decode("utf-8"))
            pages = data.get("query", {}).get("pages", {})
            for page_id, page_info in pages.items():
                if "thumbnail" in page_info:
                    thumb_url = page_info["thumbnail"]["source"]
                    img_req = urllib.request.Request(thumb_url, headers={"User-Agent": "KrishiMitraAgriApp/1.0"})
                    with urllib.request.urlopen(img_req, timeout=8) as img_resp:
                        img_bytes = img_resp.read()
                        img = Image.open(io.BytesIO(img_bytes)).convert("RGB")
                        # Crop to 320x220 center
                        w, h = img.size
                        target_ratio = 320.0 / 220.0
                        curr_ratio = float(w) / float(h)
                        if curr_ratio > target_ratio:
                            new_w = int(h * target_ratio)
                            left = (w - new_w) // 2
                            img = img.crop((left, 0, left + new_w, h))
                        else:
                            new_h = int(w / target_ratio)
                            top = (h - new_h) // 2
                            img = img.crop((0, top, w, top + new_h))
                        
                        img = img.resize((320, 220), Image.Resampling.LANCZOS)
                        img.save(out_path, "WEBP", quality=80)
                        print(f"Downloaded & optimized: {crop_id} -> {os.path.getsize(out_path)} bytes")
                        return out_path
    except Exception as e:
        print(f"Fetch failed for {crop_id} ({e}), creating botanical artwork fallback...")

    return generate_fallback_image(crop_id, fallback_color, name, emoji)

print(f"Starting download and optimization for {len(CROP_QUERIES)} crops...")
total_size = 0
for cid, (q, color, name, emoji) in CROP_QUERIES.items():
    p = fetch_and_save_crop_image(cid, q, color, name, emoji)
    total_size += os.path.getsize(p)

print(f"ALL 49 CROPS READY! Total asset size: {total_size / 1024:.1f} KB")
