"""服务子包。"""

from app.services.analyzer import AnalyzerService

analyzer_service = AnalyzerService()


async def run_analysis(request):
    """对外便捷入口：调用分析服务执行 LangGraph 工作流。"""
    return await analyzer_service.run(request)