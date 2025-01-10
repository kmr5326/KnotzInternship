from fastapi import APIRouter
from app.services.influxdb_service import get_influxdb_data
from app.models import InfluxData

data_router = APIRouter()

@data_router.get("/data", response_model=list[InfluxData])
async def get_data():
    # InfluxDB에서 데이터를 조회
    data = get_influxdb_data()
    return data
