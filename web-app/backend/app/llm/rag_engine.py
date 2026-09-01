from typing import List, Dict, Any

class RagEngine:
    """
    RAG engine providing curated ICAR and Ministry of Agriculture knowledge context
    to ground big LLMs and eliminate hallucinations.
    """

    def __init__(self):
        self.knowledge_base: List[Dict[str, Any]] = [
            {
                "title": "ICAR Wheat Cultivation & Irrigation Guidelines",
                "keywords": ["wheat", "गेहूं", "irrigation", "सिंचाई", "water", "पानी", "cri"],
                "text": "Wheat requires 4 to 6 irrigations depending on soil type. The Critical Root Initiation (CRI) stage at 20-25 days after sowing is the most critical stage. If moisture drops below 20%, immediate irrigation is required to prevent yield loss."
            },
            {
                "title": "ICAR Yellow Rust Management Advisory",
                "keywords": ["yellow rust", "पीला रतुआ", "rust", "fungus", "रोग", "पत्ती", "leaf"],
                "text": "Yellow Rust (Puccinia striiformis) appears as bright yellow stripes along leaf veins. Spray Propiconazole 25% EC @ 1 ml/litre water or Tebuconazole 25% WG @ 1 g/litre. Avoid excessive nitrogen fertilizer."
            },
            {
                "title": "Ministry of Agriculture - PM-KISAN Scheme Manual",
                "keywords": ["pm-kisan", "pm kisan", "योजना", "scheme", "6000", "सम्मान निधि"],
                "text": "Pradhan Mantri Kisan Samman Nidhi (PM-KISAN) provides ₹6,000 per year in 3 equal installments of ₹2,000 directly to bank accounts of land-holding farmer families. Requires Aadhaar linking and e-KYC on pmkisan.gov.in."
            },
            {
                "title": "NABARD - Kisan Credit Card (KCC) Guidelines",
                "keywords": ["kcc", "loan", "ऋण", "लोन", "credit", "ब्याज", "interest"],
                "text": "Kisan Credit Card (KCC) provides short-term crop loans up to ₹3 Lakh at an effective interest rate of 4% per annum (7% standard minus 3% prompt repayment incentive). Covers crop cultivation and post-harvest expenses."
            },
            {
                "title": "Digital Twin Soil Moisture & Evapotranspiration Rules",
                "keywords": ["twin", "digital twin", "ज़ोन", "zone", "moisture", "नमी", "ट्विन"],
                "text": "Field digital twins partition fields into 9 management zones. Zones with moisture below 20% under temperatures >34°C with rain probability <35% indicate acute water stress requiring prompt 35mm irrigation."
            }
        ]

    def search(self, query: str, top_k: int = 2) -> List[Dict[str, Any]]:
        """Keyword and lexical search over agricultural knowledge base"""
        q_lower = query.lower()
        scored = []
        for doc in self.knowledge_base:
            score = 0
            for kw in doc["keywords"]:
                if kw in q_lower:
                    score += 2
            if score > 0:
                scored.append((score, doc))
        scored.sort(key=lambda x: x[0], reverse=True)
        return [doc for _, doc in scored[:top_k]]

rag_engine = RagEngine()
