import base64
from io import BytesIO


def encode_image_to_base64(image: BytesIO) -> str:
    # 이미지를 Base64로 변환
    buffered = BytesIO()
    image.save(buffered, format="PNG")
    img_str = base64.b64encode(buffered.getvalue()).decode("utf-8")

    return img_str
