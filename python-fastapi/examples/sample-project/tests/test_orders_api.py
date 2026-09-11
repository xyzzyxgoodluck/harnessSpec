"""订单接口测试：成功/冲突/未找到/参数非法四条链路，全部经 ASGITransport 直连应用。"""

from httpx import AsyncClient

from sample.core.logging import TRACE_ID_HEADER
from tests.fakes import FakeCache, FakeOrderRepository


async def test_create_order_returns_201(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/orders", json={"order_no": "ORD-20260910-1001", "amount_cents": 12800}
    )

    assert response.status_code == 201
    assert response.json() == {
        "order_no": "ORD-20260910-1001",
        "status": "CREATED",
        "amount_cents": 12800,
    }
    assert response.headers[TRACE_ID_HEADER]


async def test_get_order_returns_created_order(client: AsyncClient) -> None:
    await client.post("/api/v1/orders", json={"order_no": "ORD-20260910-1002", "amount_cents": 500})

    response = await client.get("/api/v1/orders/ORD-20260910-1002")

    assert response.status_code == 200
    assert response.json()["amount_cents"] == 500


async def test_get_missing_order_returns_404_with_error_code(client: AsyncClient) -> None:
    response = await client.get("/api/v1/orders/ORD-20260910-9999")

    assert response.status_code == 404
    body = response.json()
    assert body["code"] == "300001"
    assert "ORD-20260910-9999" in body["message"]
    assert body["traceId"]


async def test_create_duplicated_order_returns_409(client: AsyncClient) -> None:
    payload = {"order_no": "ORD-20260910-1003", "amount_cents": 100}
    await client.post("/api/v1/orders", json=payload)

    response = await client.post("/api/v1/orders", json=payload)

    assert response.status_code == 409
    assert response.json()["code"] == "300002"


async def test_invalid_payload_returns_422(client: AsyncClient) -> None:
    response = await client.post(
        "/api/v1/orders", json={"order_no": "bad order no", "amount_cents": 0}
    )

    assert response.status_code == 422
    assert response.json()["code"] == "200001"


async def test_cache_invalidated_on_create(
    client: AsyncClient, fake_cache: FakeCache, fake_repository: FakeOrderRepository
) -> None:
    await client.post("/api/v1/orders", json={"order_no": "ORD-20260910-1004", "amount_cents": 100})

    assert fake_cache.deleted == ["order:detail:ORD-20260910-1004"]
