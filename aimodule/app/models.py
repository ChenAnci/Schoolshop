"""只读表模型。

仅映射分析所需的数据库视图 / 表，只读使用（不做增删改）。
对应本工程 ruoyi_vue 库中实际存在的表结构。
查询逻辑放在 workflows 节点中，本文件保持纯模型。
"""

from datetime import datetime
from decimal import Decimal

from sqlalchemy import DateTime, Numeric, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class BaseRO(DeclarativeBase):
    """只读基类。"""


class MarketOrder(BaseRO):
    """订单主表（只读）。实际来自 market_orders 表。"""

    __tablename__ = "market_orders"

    order_id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[int | None] = mapped_column()
    shop_id: Mapped[int | None] = mapped_column()
    status: Mapped[str | None] = mapped_column(String(8))
    total_amount: Mapped[Decimal | None] = mapped_column(Numeric(10, 2))
    create_time: Mapped[datetime | None] = mapped_column(DateTime())