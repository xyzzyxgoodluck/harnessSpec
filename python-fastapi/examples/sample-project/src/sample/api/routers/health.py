"""存活/就绪探针：不依赖业务逻辑，供容器编排与负载均衡使用。"""

from fastapi import APIRouter

router = APIRouter(tags=["health"])


@router.get("/healthz", summary="存活探针", description="进程存活即返回 ok，不探测下游依赖。")
async def healthz() -> dict[str, str]:
    return {"status": "ok"}
