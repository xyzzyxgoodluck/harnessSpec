"""探针与链路追踪基础行为。"""

from httpx import AsyncClient

from sample.core.logging import TRACE_ID_HEADER


async def test_healthz_returns_ok(client: AsyncClient) -> None:
    response = await client.get("/healthz")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


async def test_trace_id_is_returned_and_echoed(client: AsyncClient) -> None:
    generated = await client.get("/healthz")
    assert generated.headers[TRACE_ID_HEADER]

    echoed = await client.get("/healthz", headers={TRACE_ID_HEADER: "trace-from-client"})
    assert echoed.headers[TRACE_ID_HEADER] == "trace-from-client"
