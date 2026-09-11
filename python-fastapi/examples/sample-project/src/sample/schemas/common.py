"""通用出参模型：分页结构与统一错误结构。"""

from pydantic import BaseModel, ConfigDict, Field


class PageResult[T](BaseModel):
    """分页统一结构，禁止各接口自定义字段名。"""

    model_config = ConfigDict(extra="forbid")

    items: list[T] = Field(default_factory=list)
    total: int = Field(ge=0)
    page: int = Field(ge=1)
    page_size: int = Field(ge=1)


class ErrorResponse(BaseModel):
    """错误响应：只有 code/message/traceId，不含 data，不含堆栈。

    traceId 为对外契约字段名（与 X-Trace-Id 响应头对齐），故此处豁免 N815。
    """

    model_config = ConfigDict(extra="forbid")

    code: str
    message: str
    traceId: str  # noqa: N815
