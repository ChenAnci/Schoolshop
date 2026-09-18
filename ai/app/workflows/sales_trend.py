"""LangGraph 工作流：销售趋势分析（骨架占位实现）。

本期不接真实大模型，工作流跑一条「只读 SQL 查询 → 组装结构化结果」的
可运行链路，作为 LangGraph 编排骨架：
    query_sales -> build_points -> (可选) summarize

配置了 LLM Key 且 ANALYSIS_ENABLE_LLM=true 时，末尾会追加自然语言解读；
否则直接返回 SQL 结果。工作流与 LLM 解耦，保证骨架零依赖可运行。
"""

from datetime import date, timedelta
from typing import TypedDict

from langgraph.graph import END, START, StateGraph

from app.config import get_settings
from app.db import create_session
from app.models import MarketOrder
from app.schemas import AnalysisData, AnalysisRequest, AnalysisResponse
from sqlalchemy import func


class SalesState(TypedDict, total=False):
    request: AnalysisRequest
    points: list[dict]
    meta: dict
    ai_summary: str | None


def _resolve_range(req: AnalysisRequest) -> tuple[date, date]:
    end = req.end_date or date.today()
    start = req.start_date or (end - timedelta(days=29))
    return start, end


def _query_sales(req: AnalysisRequest) -> list[dict]:
    """只读查询已完成(2)订单按日聚合的金额与数量。"""
    start, end = _resolve_range(req)
    with create_session() as session:
        day = func.date(MarketOrder.create_time)
        rows = (
            session.query(
                day.label("day"),
                func.count(MarketOrder.order_id).label("order_count"),
                func.coalesce(func.sum(MarketOrder.total_amount), 0).label("amount"),
            )
            .filter(
                MarketOrder.status == "2",
                day >= start,
                day <= end,
            )
            .group_by(day)
            .order_by(day)
            .all()
        )
        return [
            {"day": str(row.day), "order_count": int(row.order_count), "amount": round(float(row.amount or 0), 2)}
            for row in rows
        ]


def query_sales(state: SalesState) -> SalesState:
    req = state["request"]
    points = _query_sales(req)
    total = sum(p["amount"] for p in points)
    state["points"] = points
    state["meta"] = {
        "group_by": req.group_by,
        "total_amount": round(total, 2),
        "total_orders": sum(p["order_count"] for p in points),
    }
    return state


def build_points(state: SalesState) -> SalesState:
    # 占位：后续可在 points 基础上做哑变量填充 / 趋势计算。
    # 骨架阶段直接透传 SQL 结果，保证链路完整可跑。
    return state


def maybe_summarize(state: SalesState) -> SalesState:
    """可选 LLM 解读。未开启时保持 ai_summary=None，骨架阶段可跑。"""
    settings = get_settings()
    if settings.analysis_enable_llm and settings.llm_api_key:
        # 骨架占位：真实场景在这里调用 LangChain LLM 对 state["points"] 生成摘要。
        # 未接入具体模型供应商前，返回一段模板化说明便于联调。
        state["ai_summary"] = (
            f"近 {len(state['points'])} 天共 {state['meta']['total_orders']} 单，"
            f"总金额 {state['meta']['total_amount']} 元（LLM 增强未接入，模板占位）。"
        )
    else:
        state["ai_summary"] = None
    return state


def build_sales_trend_graph():
    """编译销售趋势 LangGraph 工作流。"""
    builder = StateGraph(SalesState)
    builder.add_node("query_sales", query_sales)
    builder.add_node("build_points", build_points)
    builder.add_node("maybe_summarize", maybe_summarize)
    builder.add_edge(START, "query_sales")
    builder.add_edge("query_sales", "build_points")
    builder.add_edge("build_points", "maybe_summarize")
    builder.add_edge("maybe_summarize", END)
    return builder.compile()