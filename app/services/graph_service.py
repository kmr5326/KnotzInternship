import matplotlib.pyplot as plt
from io import BytesIO


def generate_graph(data) -> BytesIO:
    # 예시: data는 InfluxData 객체들의 리스트
    times = [item.time for item in data]
    values = [item.value for item in data]

    plt.figure(figsize=(10, 5))
    plt.plot(times, values, marker='o')
    plt.title("Time Series Data")
    plt.xlabel("Time")
    plt.ylabel("Value")
    plt.xticks(rotation=45)

    # 그래프를 이미지로 저장
    img = BytesIO()
    plt.savefig(img, format='png')
    img.seek(0)

    return img
