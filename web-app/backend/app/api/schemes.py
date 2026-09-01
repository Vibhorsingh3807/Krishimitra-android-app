from fastapi import APIRouter

router = APIRouter()

_SCHEMES = [
    {
        "id": "pm_kisan",
        "name": "PM-KISAN (Pradhan Mantri Kisan Samman Nidhi)",
        "name_hi": "प्रधानमंत्री किसान सम्मान निधि (PM-KISAN)",
        "benefit": "₹6,000 per year in 3 installments of ₹2,000 direct to bank",
        "eligibility": "All landholding farmer families across India",
        "portal_url": "https://pmkisan.gov.in",
        "category": "Direct Income Support"
    },
    {
        "id": "kcc_loan",
        "name": "Kisan Credit Card (KCC) Crop Loan",
        "name_hi": "किसान क्रेडिट कार्ड (KCC) कृषि ऋण",
        "benefit": "Collateral-free loan up to ₹1.6 Lakh (Total limit ₹3 Lakh at 4% effective interest)",
        "eligibility": "Individual/Joint borrowers, Tenant farmers, Oral lessees, SHGs",
        "portal_url": "https://agricoop.gov.in",
        "category": "Credit & Loan"
    },
    {
        "id": "pmfby",
        "name": "PMFBY (Pradhan Mantri Fasal Bima Yojana)",
        "name_hi": "प्रधानमंत्री फसल बीमा योजना (PMFBY)",
        "benefit": "Comprehensive risk insurance for crop loss due to non-preventable natural risks",
        "eligibility": "All farmers growing notified crops in notified areas",
        "portal_url": "https://pmfby.gov.in",
        "category": "Insurance"
    },
    {
        "id": "solar_kusum",
        "name": "PM-KUSUM Solar Pump Scheme",
        "name_hi": "पीएम-कुसुम सौर पंप सब्सिडी योजना",
        "benefit": "Up to 60% government subsidy for standalone solar agriculture pumps",
        "eligibility": "Farmers with agriculture land requiring tube well or irrigation pump",
        "portal_url": "https://pmkusum.mnre.gov.in",
        "category": "Energy Subsidy"
    }
]

@router.get("")
def get_schemes():
    return _SCHEMES
