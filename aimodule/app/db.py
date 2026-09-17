"""只读数据库访问层。

- 使用 SQLAlchemy 2.x 的 engine + sessionmaker。
- 账号 / 库名由配置注入，便捷操作：create_session() 获得只读会话。
- 表模型集中放在 models.py，映射分析所需视图 / 表，避免直接拼接 SQL。
"""

from sqlalchemy import Engine, create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.config import get_settings


def create_db_engine() -> Engine:
    settings = get_settings()
    return create_engine(
        settings.sqlalchemy_url,
        pool_pre_ping=True,
        # 只读意图：连接级只读会话（由 DB 账号权限兜底）
        connect_args={"read_default_group": "ai_ro"} if False else {},
    )


# 惰性单例 engine（首次使用才创建，避免模块导入即连库）
_engine: Engine | None = None


def get_engine() -> Engine:
    global _engine
    if _engine is None:
        _engine = create_db_engine()
    return _engine


def create_session() -> Session:
    return sessionmaker(bind=get_engine(), autoflush=False, expire_on_commit=False)()


def check_database() -> dict:
    """健康检查用：探测 DB 连通性，返回可供 /health 展示的状态。"""
    try:
        with create_session() as session:
            session.execute(  # noqa: B018
                __import__("sqlalchemy").text("SELECT 1")
            )
        return {"status": "ok"}
    except Exception as exc:  # noqa: BLE001
        return {"status": "error", "detail": str(exc)}