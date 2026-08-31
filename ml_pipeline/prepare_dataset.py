"""
prepare_dataset.py
Generates realistic farmer query variations across Hindi, English, and Hinglish
inspired by Government of India Kisan Call Centre (KCC) real farmer query patterns.
All answers map strictly to verified ICAR and Ministry of Agriculture facts.
"""

import json
import os
import random

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(ROOT_DIR, "data")

def load_json(filename):
    with open(os.path.join(DATA_DIR, filename), "r", encoding="utf-8") as f:
        return json.load(f)

def generate_crop_questions(crops):
    dataset = []

    # Comprehensive KCC-inspired natural farmer question templates
    templates = {
        "soil": {
            "en": [
                "What soil is suitable for {crop_en}?",
                "Which type of soil is best for {crop_en}?",
                "Can I grow {crop_en} in sandy loam or red soil?",
                "Tell me the suitable soil and pH for {crop_en}.",
                "My field has black soil, is it good for {crop_en}?"
            ],
            "hi": [
                "{crop_hi} के लिए कौन सी मिट्टी अच्छी है?",
                "{crop_hi} की खेती के लिए उपयुक्त मिट्टी व पीएच कौन सा है?",
                "मेरे खेत की मिट्टी दोमट है, क्या {crop_hi} लगा सकते हैं?",
                "{crop_hi} बोने के लिए कैसी जमीन चाहिए?",
                "{crop_hi} के लिए मिट्टी का पीएच कितना होना चाहिए?"
            ],
            "hinglish": [
                "{crop_en} ke liye kaunsi mitti achhi hai?",
                "{crop_en} cultivation ke liye soil kaisa hona chahiye?",
                "Mere khet ki mitti laal hai kya {crop_en} uga sakte hain?",
                "Kali mitti me {crop_en} ki kheti ho sakti hai kya?",
                "{crop_en} kheti ke liye best soil kaun sa hai?"
            ]
        },
        "irrigation": {
            "en": [
                "How much irrigation does {crop_en} need?",
                "When should I water {crop_en}?",
                "What are the critical irrigation stages for {crop_en}?",
                "Watering schedule for {crop_en} cultivation.",
                "How many irrigations are required for {crop_en}?"
            ],
            "hi": [
                "{crop_hi} में कितनी सिंचाई की आवश्यकता होती है?",
                "{crop_hi} को पानी कब-कब देना चाहिए?",
                "{crop_hi} में पहला पानी कब लगाएं?",
                "{crop_hi} की सिंचाई के मुख्य समय कौन से हैं?",
                "{crop_hi} में कितने दिन के अंतर पर पानी देना चाहिए?"
            ],
            "hinglish": [
                "{crop_en} me kitni sinchai karni chahiye?",
                "{crop_en} ko pani kab kab dena chahiye?",
                "{crop_en} me pehla pani kab de?",
                "{crop_en} irrigation requirement kitni hai?",
                "{crop_en} me kitne din baad pani lagana chahiye?"
            ]
        },
        "fertilizer": {
            "en": [
                "What fertilizer and NPK dose is recommended for {crop_en}?",
                "How much urea and DAP should I apply to {crop_en}?",
                "Best fertilizer schedule for {crop_en} crop.",
                "Nutrient management for {crop_en}.",
                "How much NPK per acre for {crop_en}?"
            ],
            "hi": [
                "{crop_hi} में कौन सी खाद और कितनी मात्रा में डालनी चाहिए?",
                "{crop_hi} के लिए एनपीके की अनुशंसित मात्रा क्या है?",
                "{crop_hi} में यूरिया और डीएपी कब और कितना दें?",
                "{crop_hi} की फसल में खाद प्रबंधन कैसे करें?",
                "{crop_hi} में प्रति एकड़ कितनी खाद डालें?"
            ],
            "hinglish": [
                "{crop_en} me kaunsi khad dalni chahiye?",
                "{crop_en} ke liye NPK ratio kitna hona chahiye?",
                "{crop_en} me urea aur DAP kitna dale?",
                "{crop_en} me khad kab deni chahiye?",
                "{crop_en} me fertilizer dose kitni hai?"
            ]
        },
        "sowing": {
            "en": [
                "What is the best sowing time for {crop_en}?",
                "When to sow {crop_en}?",
                "Which season is suitable for {crop_en} cultivation?",
                "Ideal planting month for {crop_en}.",
                "Can I sow {crop_en} in July or November?"
            ],
            "hi": [
                "{crop_hi} की बुवाई का सही समय क्या है?",
                "{crop_hi} कब बोई जाती है?",
                "{crop_hi} की खेती किस मौसम में करनी चाहिए?",
                "{crop_hi} लगाने का सबसे अच्छा महीना कौन सा है?",
                "{crop_hi} की अगेती बुवाई कब करें?"
            ],
            "hinglish": [
                "{crop_en} ki buwai kab karni chahiye?",
                "{crop_en} sowing time kaun sa best hai?",
                "{crop_en} lagane ka sahi samay kya hai?",
                "{crop_en} kab lagaye?",
                "{crop_en} kis mahine me boi jati hai?"
            ]
        },
        "pests_diseases": {
            "en": [
                "What pests and diseases attack {crop_en}?",
                "Major insect pests in {crop_en}.",
                "How to protect {crop_en} from insects, caterpillar and pests?",
                "Insect attack in {crop_en} field what should I spray?",
                "Pest management in {crop_en} crop."
            ],
            "hi": [
                "{crop_hi} में कौन-कौन से मुख्य रोग और कीट लगते हैं?",
                "{crop_hi} की फसल में लगने वाले प्रमुख कीड़े कौन से हैं?",
                "{crop_hi} में कीड़ा लग गया है कौन सी दवा डालें?",
                "{crop_hi} को कीटों और बीमारियों से कैसे बचाएं?",
                "{crop_hi} में सुंडी या इल्ली का नियंत्रण कैसे करें?"
            ],
            "hinglish": [
                "{crop_en} me kaunse insect aur keede lagte hai?",
                "{crop_en} me keeda lag gaya hai patte me chhed hai kya kare?",
                "{crop_en} me illi aur sundi se bachane ke liye kaun si dawai dale?",
                "{crop_en} ke major pests kya hai?",
                "{crop_en} ko bimari aur keede se kaise bachaye?"
            ]
        },
        "harvesting": {
            "en": [
                "When should I harvest {crop_en}?",
                "How to know when {crop_en} is ready for harvesting?",
                "Maturity signs and harvesting of {crop_en}.",
                "Harvesting time for {crop_en}.",
                "How many days does {crop_en} take to mature?"
            ],
            "hi": [
                "{crop_hi} की कटाई कब और कैसे करनी चाहिए?",
                "{crop_hi} पकने की क्या पहचान है?",
                "{crop_hi} कितने दिन में पक कर तैयार हो जाती है?",
                "{crop_hi} की तुड़ाई का सही समय क्या है?",
                "{crop_hi} की फसल तैयार होने के लक्षण क्या हैं?"
            ],
            "hinglish": [
                "{crop_en} ki katai kab karni chahiye?",
                "{crop_en} harvesting time kya hai?",
                "{crop_en} pakne ki kya pehchan hai?",
                "{crop_en} kitne din me pakti hai?",
                "{crop_en} ki tudayi kab kare?"
            ]
        },
        "cultivation": {
            "en": [
                "How to cultivate {crop_en}?",
                "Complete guide for growing {crop_en}.",
                "Best practices for {crop_en} farming.",
                "Tips for high yield in {crop_en}."
            ],
            "hi": [
                "{crop_hi} की खेती कैसे करें?",
                "{crop_hi} की पूरी वैज्ञानिक खेती विधि बताएं।",
                "{crop_hi} की उन्नत खेती के उपाय क्या हैं?",
                "{crop_hi} से अधिक पैदावार कैसे प्राप्त करें?"
            ],
            "hinglish": [
                "{crop_en} ki kheti kaise kare?",
                "{crop_en} farming tips bataye?",
                "{crop_en} ki acchi paidaawar kaise le?",
                "{crop_en} ki scientific kheti kaise hoti hai?"
            ]
        },
        "seed_treatment": {
            "en": [
                "How do I treat {crop_en} seeds before sowing?",
                "What is the seed treatment protocol for {crop_en}?",
                "Seed rate and seed dressing for {crop_en}.",
                "How to treat {crop_en} seeds with fungicide?"
            ],
            "hi": [
                "{crop_hi} की बुवाई से पहले बीज उपचार कैसे करें?",
                "{crop_hi} के बीज को किस दवा से उपचारित करें?",
                "{crop_hi} की बीज दर और बीजोपचार विधि क्या है?",
                "{crop_hi} के बीज का शोधन कैसे करें?"
            ],
            "hinglish": [
                "{crop_en} me seed treatment kaise kare?",
                "{crop_en} ka beej upchar kisse kare?",
                "{crop_en} ke beej ko boney se pehle kis dawai se upcharit kare?",
                "{crop_en} ki seed rate kitni hai?"
            ]
        },
        "weed_management": {
            "en": [
                "How to control weeds in {crop_en} field?",
                "Which herbicide should I spray in {crop_en}?",
                "Weed management schedule for {crop_en}.",
                "How to remove grass and weeds in {crop_en}?"
            ],
            "hi": [
                "{crop_hi} में खरपतवार नियंत्रण कैसे करें?",
                "{crop_hi} में कौन सी खरपतवार नाशक दवा डालें?",
                "{crop_hi} में घास और कचरा नष्ट करने के उपाय बताएं।",
                "{crop_hi} में निराई-गुड़ाई कब करनी चाहिए?"
            ],
            "hinglish": [
                "{crop_en} me weed control kaise kare?",
                "{crop_en} me ghaas marne ki dawai kaun si hai?",
                "{crop_en} me kachra kaise saaf kare?",
                "{crop_en} me herbicide kaun sa dale?"
            ]
        },
        "spacing": {
            "en": [
                "What is the plant spacing for {crop_en}?",
                "Row to row and plant to plant distance for {crop_en}.",
                "Plant population and spacing for {crop_en}."
            ],
            "hi": [
                "{crop_hi} की बुवाई में कतार से कतार की दूरी कितनी रखें?",
                "{crop_hi} में पौधों के बीच की सही दूरी क्या होनी चाहिए?",
                "{crop_hi} में लाइन से लाइन का फासला कितना होना चाहिए?"
            ],
            "hinglish": [
                "{crop_en} me plant spacing kitni honi chahiye?",
                "{crop_en} me row to row distance kitna rakhe?",
                "{crop_en} me line se line aur paudhe ki doori kitni rakhe?"
            ]
        },
        "storage": {
            "en": [
                "How to store {crop_en} safely after harvesting?",
                "Safe grain storage moisture level for {crop_en}.",
                "Post-harvest storage tips for {crop_en}.",
                "How to protect stored {crop_en} from grain pests?"
            ],
            "hi": [
                "{crop_hi} का सुरक्षित भंडारण कैसे करें?",
                "{crop_hi} के भंडारण में नमी कितनी होनी चाहिए?",
                "{crop_hi} को कीटों और घुन से बचाने के लिए कैसे भंडारित करें?",
                "{crop_hi} को कोठी या गोदाम में कैसे सुरक्षित रखें?"
            ],
            "hinglish": [
                "{crop_en} ka storage kaise kare?",
                "{crop_en} store karte time kitni moisture honi chahiye?",
                "{crop_en} ko gudam me ghun aur keet se kaise bachaye?"
            ]
        },
        "crop_rotation": {
            "en": [
                "Which crop can I plant after {crop_en}?",
                "Best crop rotation with {crop_en}.",
                "Intercropping options with {crop_en}.",
                "Can I intercrop pulses with {crop_en}?"
            ],
            "hi": [
                "{crop_hi} के बाद कौन सी फसल लगानी चाहिए?",
                "{crop_hi} कटने के बाद खेत में क्या लगाएं?",
                "{crop_hi} के साथ कौन सी अंतःफसल लगा सकते हैं?",
                "{crop_hi} के लिए सही फसल चक्र क्या है?"
            ],
            "hinglish": [
                "{crop_en} ke baad kaunsi fasal lagaye?",
                "{crop_en} katne ke baad khet me kya lagaye?",
                "{crop_en} ke sath intercropping kya kare?",
                "{crop_en} ke sath kaunsi fasal bo sakte hain?"
            ]
        },
        "nutrient_deficiency": {
            "en": [
                "My {crop_en} leaves are turning yellow what should I do?",
                "Nutrient deficiency symptoms in {crop_en}.",
                "Why are {crop_en} leaves yellowing or drying?",
                "Yellowing of leaves in {crop_en} remedy."
            ],
            "hi": [
                "मेरे {crop_hi} के पत्ते पीले पड़ रहे हैं क्या करें?",
                "{crop_hi} में पोषक तत्वों की कमी के लक्षण क्या हैं?",
                "{crop_hi} की पत्तियां पीली होकर सूख रही हैं क्या उपाय है?",
                "{crop_hi} में जिंक या सल्फर की कमी कैसे पहचानें?"
            ],
            "hinglish": [
                "Mere {crop_en} ke patte peele ho rahe hai kya karun?",
                "{crop_en} me patte sukh rahe hain aur peele pad rahe hain",
                "{crop_en} me nutrient deficiency symptoms kya hai?",
                "{crop_en} me peelapan kaise door kare?"
            ]
        }
    }

    for crop in crops:
        c_id = crop["id"]
        c_en = crop["name_en"]
        c_hi = crop["name_hi"]

        # 1. Soil
        ans_en = f"For {c_en}, suitable soil is {crop['soil']} with pH {crop.get('soil_ph', '6.0-7.5')}. Climate required: {crop.get('climate', 'tropical/subtropical')}."
        ans_hi = f"{c_hi} के लिए उपयुक्त मिट्टी: {crop['soil_hi']} (पीएच मान: {crop.get('soil_ph', '6.0-7.5')})। जलवायु: {crop.get('climate_hi', 'अनुकूल जलवायु')}।"
        for lang, qs in templates["soil"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "soil", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 2. Irrigation
        ans_en = f"Irrigation for {c_en}: {crop['irrigation']}"
        ans_hi = f"{c_hi} की सिंचाई: {crop['irrigation_hi']}"
        for lang, qs in templates["irrigation"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "irrigation", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 3. Fertilizer
        ans_en = f"Fertilizer dose for {c_en}: {crop['fertilizer']}"
        ans_hi = f"{c_hi} के लिए खाद व उर्वरक: {crop['fertilizer_hi']}"
        for lang, qs in templates["fertilizer"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "fertilizer", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 4. Sowing
        ans_en = f"Sowing season for {c_en}: {crop['sowing_season']}. Temperature: {crop.get('temperature', '20-30°C')}."
        ans_hi = f"{c_hi} की बुवाई का समय: {crop['sowing_season_hi']}। उपयुक्त तापमान: {crop.get('temperature', '20-30°C')}।"
        for lang, qs in templates["sowing"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "sowing", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 5. Pests & Diseases
        ans_en = f"Common pests in {c_en}: {crop['pests']}. Common diseases: {crop['diseases']}."
        ans_hi = f"{c_hi} में प्रमुख कीट: {crop['pests_hi']}। प्रमुख रोग: {crop['diseases_hi']}।"
        for lang, qs in templates["pests_diseases"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "pests_diseases", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 6. Harvesting
        ans_en = f"Harvesting for {c_en}: {crop['harvesting']}"
        ans_hi = f"{c_hi} की कटाई: {crop['harvesting_hi']}"
        for lang, qs in templates["harvesting"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "harvesting", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 7. Cultivation Tips
        ans_en = f"Cultivation tips for {c_en}: {crop['cultivation_tips']}"
        ans_hi = f"{c_hi} की उन्नत खेती सलाह: {crop['cultivation_tips_hi']}"
        for lang, qs in templates["cultivation"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "cultivation", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 8. Seed treatment
        for lang, qs in templates["seed_treatment"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "seed_treatment", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 9. Weed management
        for lang, qs in templates["weed_management"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "weed_management", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 10. Spacing
        for lang, qs in templates["spacing"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "spacing", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 11. Storage
        for lang, qs in templates["storage"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "storage", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 12. Crop rotation
        for lang, qs in templates["crop_rotation"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "crop_rotation", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

        # 13. Nutrient deficiency
        for lang, qs in templates["nutrient_deficiency"].items():
            for q in qs:
                dataset.append({"question": q.format(crop_en=c_en, crop_hi=c_hi), "lang": lang, "intent": "nutrient_deficiency", "crop_id": c_id, "answer_en": ans_en, "answer_hi": ans_hi, "source": crop["source"]})

    return dataset

def generate_scheme_questions(schemes):
    dataset = []
    for s in schemes:
        ans_en = f"Scheme: {s['name_en']}. Ministry: {s['ministry']}. Benefits: {s['benefits_en']}. Eligibility: {s['eligibility_en']}. Application: {s['application_process_en']}. Official portal: {s['official_url']}."
        ans_hi = f"योजना: {s['name_hi']}। मंत्रालय: {s['ministry']}। लाभ: {s['benefits_hi']}। पात्रता: {s['eligibility_hi']}। आवेदन प्रक्रिया: {s['application_process_hi']}। आधिकारिक पोर्टल: {s['official_url']}।"

        s_en = s["name_en"]
        s_hi = s["name_hi"]

        queries_en = [
            f"What is {s_en}?",
            f"How to apply for {s_en}?",
            f"Eligibility and benefits of {s_en}.",
            f"What documents are needed for {s_en}?"
        ]
        queries_hi = [
            f"{s_hi} क्या है?",
            f"{s_hi} के लिए आवेदन कैसे करें?",
            f"{s_hi} के क्या लाभ और पात्रता है?",
            f"{s_hi} में कितना पैसा या सब्सिडी मिलती है?"
        ]
        queries_hinglish = [
            f"{s_en} scheme kya hai?",
            f"{s_en} me apply kaise kare?",
            f"{s_en} ke benefits aur eligibility kya hai?"
        ]

        for q in queries_en:
            dataset.append({"question": q, "lang": "en", "intent": "schemes", "crop_id": None, "answer_en": ans_en, "answer_hi": ans_hi, "source": s["source"]})
        for q in queries_hi:
            dataset.append({"question": q, "lang": "hi", "intent": "schemes", "crop_id": None, "answer_en": ans_en, "answer_hi": ans_hi, "source": s["source"]})
        for q in queries_hinglish:
            dataset.append({"question": q, "lang": "hinglish", "intent": "schemes", "crop_id": None, "answer_en": ans_en, "answer_hi": ans_hi, "source": s["source"]})

    return dataset

def generate_loan_questions(loans):
    dataset = []
    for l in loans:
        ans_en = f"Loan: {l['loan_type']} by {l['bank_name']}. Purpose: {l['purpose_en']}. Interest Rate: {l['interest_rate']}. Max Limit: {l['max_limit']}. Eligibility: {l['eligibility_en']}. Documents: {l['documents_required']}. Details: {l['official_url']}."
        ans_hi = f"ऋण: {l['loan_type_hi']} ({l['bank_name_hi']})। उद्देश्य: {l['purpose_hi']}। ब्याज दर: {l['interest_rate_hi']}। अधिकतम सीमा: {l['max_limit_hi']}। पात्रता: {l['eligibility_hi']}। आवश्यक दस्तावेज: {l['documents_required_hi']}। पोर्टल: {l['official_url']}।"

        l_en = l["loan_type"]
        l_hi = l["loan_type_hi"]

        queries_en = [
            f"What is {l_en} by {l['bank_name']}?",
            f"How to get {l_en}?",
            f"Interest rate and limit for {l_en}.",
            f"Documents required for {l_en}."
        ]
        queries_hi = [
            f"{l_hi} कैसे मिलेगा?",
            f"{l_hi} की ब्याज दर और अधिकतम सीमा क्या है?",
            f"{l_hi} के लिए कौन से दस्तावेज चाहिए?",
            f"{l['bank_name_hi']} से कृषि लोन कैसे लें?"
        ]
        queries_hinglish = [
            f"{l_en} ka interest rate kitna hai?",
            f"{l_en} kaise apply kare?",
            f"{l_en} ke liye documents kya chahiye?"
        ]

        for q in queries_en:
            dataset.append({"question": q, "lang": "en", "intent": "loans", "crop_id": None, "answer_en": ans_en, "answer_hi": ans_hi, "source": l["source"]})
        for q in queries_hi:
            dataset.append({"question": q, "lang": "hi", "intent": "loans", "crop_id": None, "answer_en": ans_en, "answer_hi": ans_hi, "source": l["source"]})
        for q in queries_hinglish:
            dataset.append({"question": q, "lang": "hinglish", "intent": "loans", "crop_id": None, "answer_en": ans_en, "answer_hi": ans_hi, "source": l["source"]})

    return dataset

def generate_disease_questions(diseases):
    dataset = []
    for d in diseases:
        if d["id"] in ["soil_or_background", "uncertain_quality"]:
            continue
        ans_en = f"Disease: {d['disease_name_en']} in {d['crop']}. Pathogen: {d.get('pathogen', 'N/A')}. Symptoms: {d['symptoms_en']}. Organic treatment: {d['treatment_organic_en']}. Chemical treatment: {d['treatment_chemical_en']}. Prevention: {d['prevention_en']}."
        ans_hi = f"रोग: {d['disease_name_hi']} ({d['crop_hi']})। रोगाणु: {d.get('pathogen', 'फफूंद/जीवाणु')}। लक्षण: {d['symptoms_hi']}। जैविक उपचार: {d['treatment_organic_hi']}। रासायनिक उपचार: {d['treatment_chemical_hi']}। रोकथाम: {d['prevention_hi']}।"

        d_en = d['disease_name_en']
        d_hi = d['disease_name_hi']

        queries_en = [
            f"What is {d_en} in {d['crop']}?",
            f"How to control {d_en} in {d['crop']}?",
            f"Symptoms and cure of {d_en}.",
            f"Treatment for {d_en} disease."
        ]
        queries_hi = [
            f"{d['crop_hi']} में {d_hi} के क्या लक्षण हैं?",
            f"{d_hi} का उपचार क्या है?",
            f"{d['crop_hi']} में {d_hi} की रोकथाम कैसे करें?",
            f"{d_hi} के लिए कौन सी दवा छिड़कें?"
        ]
        queries_hinglish = [
            f"{d['crop']} me {d_en} ki dawai bataye?",
            f"{d_en} bimari ka ilaj kya hai?",
            f"{d_en} disease control kaise kare?"
        ]

        for q in queries_en:
            dataset.append({"question": q, "lang": "en", "intent": "disease", "crop_id": d["crop"].lower(), "answer_en": ans_en, "answer_hi": ans_hi, "source": "ICAR Plant Pathology Guidelines"})
        for q in queries_hi:
            dataset.append({"question": q, "lang": "hi", "intent": "disease", "crop_id": d["crop"].lower(), "answer_en": ans_en, "answer_hi": ans_hi, "source": "ICAR Plant Pathology Guidelines"})
        for q in queries_hinglish:
            dataset.append({"question": q, "lang": "hinglish", "intent": "disease", "crop_id": d["crop"].lower(), "answer_en": ans_en, "answer_hi": ans_hi, "source": "ICAR Plant Pathology Guidelines"})

    return dataset

def generate_general_agri_questions():
    """Generates cross-crop natural questions: crop selection, organic farming, solar pump, etc."""
    dataset = []

    # 1. Crop Selection (Low water, drought hardy crops)
    cs_ans_en = "For low water and rainfed conditions, ideal crops are Millets (Bajra, Jowar, Ragi), Pulses (Moong, Chickpea, Arhar, Lentil), and Oilseeds (Mustard, Safflower, Sesame). These require only 250-450 mm water and tolerate dry spells."
    cs_ans_hi = "कम पानी और सूखे क्षेत्रों के लिए उपयुक्त फसलें: श्री अन्न/मोटा अनाज (बाजरा, ज्वार, रागी), दलहन (मूंग, चना, अरहर, मसूर) और तिलहन (सरसों, कुसुम, तिल)। ये फसलें मात्र 250-450 मिमी पानी में अच्छी उपज देती हैं।"

    for q in ["Which crop needs less water?", "Best crops for low rainfall areas?", "Drought resistant crops for farming.", "कम पानी में कौन सी फसल उगाएं?", "सूखे क्षेत्रों के लिए सबसे अच्छी फसल कौन सी है?", "kam pani me kaunsi fasal lagaye?", "drought me kaunsi crop achhi hai?"]:
        dataset.append({"question": q, "lang": "auto", "intent": "crop_selection", "crop_id": None, "answer_en": cs_ans_en, "answer_hi": cs_ans_hi, "source": "ICAR Central Research Institute for Dryland Agriculture (CRIDA)"})

    # 2. Organic Farming & Bio-Inputs
    org_ans_en = "To prepare Jeevamrutha: Mix 10 kg fresh desi cow dung + 10 L cow urine + 2 kg jaggery + 2 kg pulse flour (Besan) + handful of virgin farm soil in 200 L water. Stir twice daily and ferment for 48-72 hours. Apply 200 L/acre with irrigation water for rich soil microbes."
    org_ans_hi = "जीवामृत बनाने की विधि: 200 लीटर पानी में 10 किग्रा देसी गाय का गोबर + 10 लीटर गोमूत्र + 2 किग्रा गुड़ + 2 किग्रा बेसन + 1 मुट्ठी खेत की मेड़ की मिट्टी मिलाएं। 48-72 घंटे छाया में रखकर दिन में दो बार चलाएं। प्रति एकड़ 200 लीटर जीवामृत सिंचाई के साथ दें।"

    for q in ["How to prepare Jeevamrutha?", "How to make organic bio-fertilizer at home?", "Organic farming inputs preparation.", "जीवामृत कैसे बनाएं?", "घर पर जैविक खाद और कीटनाशक बनाने की विधि।", "jeevamrut kaise banaye?", "organic khad banane ka tarika."]:
        dataset.append({"question": q, "lang": "auto", "intent": "organic_farming", "crop_id": None, "answer_en": org_ans_en, "answer_hi": org_ans_hi, "source": "National Centre of Organic and Natural Farming (NCONF), Ghaziabad"})

    # 3. Farm Machinery & Solar Pumps (PM-KUSUM)
    sm_ans_en = "Under PM-KUSUM, farmers receive up to 60% composite subsidy (30% Central + 30% State Govt) for installing standalone solar agricultural pumps (3 HP to 7.5 HP) and solarizing existing grid pumps. Apply via your state renewable energy development agency portal (e.g. UPNEDA, MEDA, RREC)."
    sm_ans_hi = "पीएम-कुसुम (PM-KUSUM) योजना के तहत 3 एचपी से 7.5 एचपी तक के सोलर पंप लगाने पर 60% तक का अनुदान (30% केंद्र + 30% राज्य सरकार) मिलता है। किसान को केवल 10% लागत देनी होती है। राज्य के अक्षय ऊर्जा पोर्टल पर आवेदन करें।"

    for q in ["How to get subsidy on solar pump?", "PM KUSUM solar pump subsidy details.", "सोलर पंप पर कितनी सब्सिडी मिलती है?", "पीएम कुसुम योजना में सोलर पंप कैसे लगवाएं?", "solar pump subsidy kaise milegi?", "PM kusum scheme me apply kaise kare?"]:
        dataset.append({"question": q, "lang": "auto", "intent": "farm_machinery", "crop_id": None, "answer_en": sm_ans_en, "answer_hi": sm_ans_hi, "source": "Ministry of New and Renewable Energy (MNRE), Govt. of India"})

    return dataset

def generate_out_of_scope_questions():
    """Generates non-agricultural queries for domain guardrail."""
    dataset = []
    redirect_en = "I am KrishiMitra, your digital agriculture assistant. I can only help with crops, soil, irrigation, fertilizer, pests, diseases, livestock, and government farming schemes. Please ask a farming-related question."
    redirect_hi = "मैं कृषिमित्र (KrishiMitra) हूँ। मैं केवल कृषि, फसल, मिट्टी, खाद, सिंचाई, कीट-रोग, पशुपालन और किसान योजनाओं से संबंधित प्रश्नों में आपकी सहायता कर सकता हूँ। कृपया खेती से जुड़ा कोई प्रश्न पूछें।"

    non_agri_queries = [
        "Who is the prime minister of India?",
        "Write a python script for sorting an array.",
        "What is the capital of France?",
        "How to book movie tickets online?",
        "Tell me a joke.",
        "Who won the cricket world cup?",
        "How to lose weight fast?",
        "What is artificial intelligence?",
        "भारत के प्रधानमंत्री कौन हैं?",
        "मुझे गाना सुनाओ।",
        "क्रिकेट मैच का स्कोर क्या है?",
        "गाड़ी कैसे चलाएं?",
        "गणित का सवाल हल करो।",
        "movie download kaise kare?",
        "python code likh ke do",
        "cricket match kisne jeeta?"
    ]

    for q in non_agri_queries:
        dataset.append({"question": q, "lang": "auto", "intent": "out_of_scope", "crop_id": None, "answer_en": redirect_en, "answer_hi": redirect_hi, "source": "KrishiMitra Domain Guardrail"})

    return dataset

def main():
    os.makedirs(DATA_DIR, exist_ok=True)
    crops = load_json("verified_crops.json")
    schemes = load_json("verified_schemes.json")
    loans = load_json("verified_loans.json")
    diseases = load_json("verified_diseases.json")

    print(f"Loaded {len(crops)} crops, {len(schemes)} schemes, {len(loans)} loans, {len(diseases)} diseases.")

    crop_qs = generate_crop_questions(crops)
    scheme_qs = generate_scheme_questions(schemes)
    loan_qs = generate_loan_questions(loans)
    disease_qs = generate_disease_questions(diseases)
    gen_qs = generate_general_agri_questions()
    oos_qs = generate_out_of_scope_questions()

    all_data = crop_qs + scheme_qs + loan_qs + disease_qs + gen_qs + oos_qs
    random.seed(42)
    random.shuffle(all_data)

    output_file = os.path.join(DATA_DIR, "generated_training_questions.json")
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)

    print(f"Generated {len(all_data)} question-answer pairs across {len(crops)} crops.")
    print(f"Saved to {output_file}")

if __name__ == "__main__":
    main()
