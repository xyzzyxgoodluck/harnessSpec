"""运行期配置：单一来源，密钥一律经环境变量注入（禁止入库、入代码、入日志）。"""

from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """环境变量前缀 SAMPLE_；本地开发用 .env（不入库），生产用部署平台的 Secret 注入。"""

    model_config = SettingsConfigDict(
        env_prefix="SAMPLE_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    app_name: str = "sample-project"
    debug: bool = False

    database_url: str = "postgresql+asyncpg://localhost:5432/sample"
    redis_url: str = "redis://localhost:6379/0"
    rabbitmq_url: str = "amqp://localhost:5672/"

    order_cache_ttl_seconds: int = 600
    sql_echo: bool = False


@lru_cache
def get_settings() -> Settings:
    """进程内单例：环境变量只解析一次，避免请求期反复读环境。"""
    return Settings()
