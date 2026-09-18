"""应用配置。

统一从环境变量 / .env 读取，供 FastAPI 与各模块使用。
"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # 服务
    ai_port: int = 8090
    ai_host: str = "0.0.0.0"

    # 只读数据库（MySQL，本工程 ruoyi_vue 库）
    ai_db_host: str = "127.0.0.1"
    ai_db_port: int = 3306
    ai_db_user: str = "root"
    ai_db_password: str = ""
    ai_db_name: str = "ruoyi_vue"

    # 内部鉴权令牌
    ai_internal_token: str = "dev-internal-token"

    # LLM 增强（可选，默认关闭）
    analysis_enable_llm: bool = False
    llm_api_key: str = ""
    llm_model: str = "gpt-4o-mini"
    llm_base_url: str = ""

    @property
    def sqlalchemy_url(self) -> str:
        """构造 SQLAlchemy 连接串（只读）。"""
        return (
            f"mysql+pymysql://{self.ai_db_user}:{self.ai_db_password}"
            f"@{self.ai_db_host}:{self.ai_db_port}/{self.ai_db_name}"
            "?charset=utf8mb4"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()