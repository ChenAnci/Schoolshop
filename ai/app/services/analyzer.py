"""对外分析服务：将请求分发到对应 LangGraph 工作流并返回契约结果。"""

from app.schemas import AnalysisData, AnalysisRequest, AnalysisResponse
from app.workflows.sales_trend import build_sales_trend_graph


class AnalyzerService:
    """分析服务。

    本期仅落地 sales_trend 工作流；新增指标在此扩充分发。
    """

    async def run(self, request: AnalysisRequest) -> AnalysisResponse:
        if request.metric == "sales_trend":
            return await self._run_sales_trend(request)
        # 其余指标为占位，返回空结构（契约不变）
        return AnalysisResponse(
            code=400,
            metric=request.metric,
            data=AnalysisData(metric=request.metric),
            ai_summary=None,
        )

    async def _run_sales_trend(self, request: AnalysisRequest) -> AnalysisResponse:
        graph = build_sales_trend_graph()
        state = await graph.ainvoke({"request": request})
        return AnalysisResponse(
            code=200,
            metric=request.metric,
            data=AnalysisData(
                metric=request.metric,
                points=state.get("points", []),
                meta=state.get("meta", {}),
            ),
            ai_summary=state.get("ai_summary"),
        )