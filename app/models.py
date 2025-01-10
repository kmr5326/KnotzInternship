from pydantic import BaseModel
from typing import List

class InfluxData(BaseModel): # 가져오는 data에 맞게 수정해야함
    time: str
    value: float

class GraphResponse(BaseModel): # 이미지 갯수가 여러개인 경우 생각해야함
    image: str  # Base64로 인코딩된 이미지
