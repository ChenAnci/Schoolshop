"""Pydantic DTO：/analysis 的入参/出参契约。"""

from datetime import date
from typing import Any, Literal

from pydantic import BaseModel, Field


# ===== 入参 =====

# 支持的指标类型（本期先落地 sales_trend，其余为占位预留）
MetricName = Literal["sales_trend"]


class AnalysisRequest(BaseModel):
    """数据分析入参契约。"""

    metric: MetricName = Field(..., description="指标类型，本期支持 sales_trend")
    start_date: date | None = Field(default=None, description="开始日期，默认近30天")
    end_date: date | None = Field(default=None, description="结束日期，默认今天")
    group_by: str = Field(default="day", description="聚合维度，如 day/week/month/shop")
    options: dict[str, Any] = Field(default_factory=dict, description="扩展参数")


# ===== 出参 =====


class HealthData(BaseModel):
    """健康检查出参。"""

    status: str
    app: str
    version: str
    database: dict


class AnalysisData(BaseModel):
    """分析结果主体（结构化）。"""

    metric: str
    points: list[dict[str, Any]] = Field(default_factory=list)
    meta: dict[str, Any] = Field(default_factory=dict)


class AnalysisResponse(BaseModel):
    """数据分析出参契约。"""

    code: int
    metric: str
    data: AnalysisData
    ai_summary: str | None = Field(default=None, description="可选 LLM 解读，骨架阶段为 null")