"""订单入参/出参模型：校验规则写在 schema，业务判断留在 service。"""

from pydantic import BaseModel, ConfigDict, Field


class OrderCreateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    order_no: str = Field(
        min_length=6,
        max_length=32,
        pattern=r"^[A-Z0-9-]+$",
        description="订单号，大写字母/数字/连字符",
        examples=["ORD-20260910-0001"],
    )
    amount_cents: int = Field(gt=0, le=100_000_000, description="金额（分）")


class OrderResponse(BaseModel):
    """出参不允许直接暴露 ORM 实体，字段显式声明。"""

    model_config = ConfigDict(from_attributes=True, extra="forbid")

    order_no: str
    status: str
    amount_cents: int
