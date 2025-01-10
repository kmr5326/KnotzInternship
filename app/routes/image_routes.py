from fastapi import APIRouter
from app.services.graph_service import generate_graph
from app.services.image_service import encode_image_to_base64
from app.services.influxdb_service import get_influxdb_data
from app.models import GraphResponse

image_router = APIRouter()


@image_router.get("/image", response_model=GraphResponse)
async def get_image():
    # 데이터를 가져와서 그래프 생성
    data = get_influxdb_data()
    graph_image = generate_graph(data)

    # 그래프 이미지를 Base64로 변환
    image_base64 = encode_image_to_base64(graph_image)

    return GraphResponse(image=image_base64)
