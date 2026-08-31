from fastapi import APIRouter
from backend.app.api.v1 import health, crops, diseases, schemes, loans, weather, ai, sync, telephony, market

api_router = APIRouter()

api_router.include_router(health.router, tags=["Health"])
api_router.include_router(crops.router, prefix="/crops", tags=["Crops"])
api_router.include_router(diseases.router, prefix="/diseases", tags=["Diseases"])
api_router.include_router(schemes.router, prefix="/schemes", tags=["Schemes"])
api_router.include_router(loans.router, prefix="/loans", tags=["Loans"])
api_router.include_router(weather.router, prefix="/weather", tags=["Weather"])
api_router.include_router(ai.router, prefix="/ai", tags=["AI Assistant"])
api_router.include_router(sync.router, prefix="/sync", tags=["Sync"])
api_router.include_router(telephony.router, prefix="/telephony", tags=["Telephony"])
api_router.include_router(market.router, prefix="/market-prices", tags=["Market Prices"])

