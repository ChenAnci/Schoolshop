"""FastAPI 应用入口。

装配版本、CORS、路由（/health、/analysis）。
启动：uvicorn app.main:app --host 0.0.0.0 --port ${AI_PORT:-8090}
"""

from fastapi import FastAPI

from app import __version__
from app.config import get_settings
from app.routers import analysis, health

settings = get_settings()

app = FastAPI(
    title="Campus AI Module",
    description="校园市场系统 AI 分析服务（FastAPI + LangChain + LangGraph 骨架）",
    version=__version__,
)

app.include_router(health.router)
app.include_router(analysis.router)


@app.get("/")
def root() -> dict:
    return {"app": "campus-ai-module", "version": __version__, "docs": "/docs"}