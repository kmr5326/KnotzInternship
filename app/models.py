from pydantic import BaseModel
from typing import List

class InfluxData(BaseModel):
    time: str
    value: float

class GraphResponse(BaseModel):
    image: str  # Base64로 인코딩된 이미지
