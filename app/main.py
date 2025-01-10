from fastapi import FastAPI
from .routes.data_routes import data_router  # 데이터 조회 라우터
from .routes.image_routes import image_router  # 이미지 반환 라우터

# FastAPI 애플리케이션 인스턴스 생성
app = FastAPI()

# 라우터들을 FastAPI 애플리케이션에 포함
app.include_router(data_router)
app.include_router(image_router)


@app.get("/")
async def read_root():
    return {"message": "Welcome to the FastAPI application!"}
