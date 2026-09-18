"""AI 模块启动入口。

直接运行：python run.py
或委托给 uvicorn：uvicorn app.main:app --reload
"""

import uvicorn

from app.config import get_settings


def main() -> None:
    settings = get_settings()
    uvicorn.run(
        "app.main:app",
        host=settings.ai_host,
        port=settings.ai_port,
        reload=False,
    )


if __name__ == "__main__":
    main()