"""POST /analysis 数据分析接口（占位实现）。"""

from fastapi import APIRouter, Depends, Header, HTTPException

from app.config import get_settings
from app.schemas import AnalysisRequest, AnalysisResponse
from app.services import analyzer_service

router = APIRouter(tags=["analysis"])


def _verify_token(authorization: str | None = Header(default=None)) -> None:
    """内部令牌校验（X-AI-Token / Authorization: Bearer）。"""
    settings = get_settings()
    expected = settings.ai_internal_token
    token = None
    if authorization:
        if authorization.lower().startswith("bearer "):
            token = authorization[7:].strip()
        else:
            token = authorization.strip()
    if not token or token != expected:
        raise HTTPException(status_code=401, detail="invalid ai token")


@router.post("/analysis", response_model=AnalysisResponse)
async def analysis(
    payload: AnalysisRequest,
    authorization: str | None = Header(default=None),
) -> AnalysisResponse:
    _verify_token(authorization)
    return await analyzer_service.run(payload)