"""create t_order

Revision ID: 0001
Revises:
Create Date: 2026-09-10

幂等前提：order_no 唯一索引是"重复创建"的业务兜底（应用层查重之外的第二道防线）。
"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0001"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

ORDER_STATUS_VALUES = ("CREATED", "PAID", "CANCELLED")


def upgrade() -> None:
    op.execute("CREATE TYPE order_status AS ENUM ('CREATED', 'PAID', 'CANCELLED')")
    op.create_table(
        "t_order",
        sa.Column("id", sa.BigInteger(), autoincrement=True, nullable=False),
        sa.Column("order_no", sa.String(length=32), nullable=False),
        sa.Column(
            "status",
            postgresql.ENUM(*ORDER_STATUS_VALUES, name="order_status", create_type=False),
            nullable=False,
        ),
        sa.Column("amount_cents", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_t_order_order_no", "t_order", ["order_no"], unique=True)


def downgrade() -> None:
    op.drop_index("ix_t_order_order_no", table_name="t_order")
    op.drop_table("t_order")
    op.execute("DROP TYPE IF EXISTS order_status")
