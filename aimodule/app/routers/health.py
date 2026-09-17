"""GET /health 健康检查。"""

from fastapi import APIRouter

from app import __version__
from app.config import get_settings
from app.db import check_database
from app.schemas import HealthData

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthData)
def health() -> HealthData:
    settings = get_settings()
    return HealthData(
        status="ok",
        app="campus-ai-module",
        version=__version__,
        database=check_database(),
    )