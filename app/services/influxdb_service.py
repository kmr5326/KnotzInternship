from typing import List
from influxdb_client import InfluxDBClient
from app.models import InfluxData



# InfluxDB 연결 설정
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "3xeAFDqD_US12X64eyyDOjAbCxM0Vjk5uAnWL7CNirlliTRtv9xdAXJee5O0EKGOS-nc2--i70SOcpnOxUUXiQ=="
INFLUXDB_ORG = "knotz"
INFLUXDB_BUCKET = "bucket1"

client = InfluxDBClient(url=INFLUXDB_URL, token=INFLUXDB_TOKEN, org=INFLUXDB_ORG)
query_api = client.query_api()


def get_influxdb_data() -> List[InfluxData]:
    query = f'''
    from(bucket: "{INFLUXDB_BUCKET}")
        |> range(start: -1h)  # 최근 1시간의 데이터 가져오기
        |> filter(fn: (r) => r["_measurement"] == "load_test_results")
        |> filter(fn: (r) => r["_field"] == "is_success")
        |> filter(fn: (r) => r["_field"] == "latency_nano")
        |> filter(fn: (r) => r["_field"] == "response_time_nano")
        |> filter(fn: (r) => r["api"] == "대상 사용자 정보를 조회한다.")
    '''
    result = query_api.query(org=INFLUXDB_ORG, query=query)

    data = []
    for table in result:
        for record in table.records:
            data.append(InfluxData(time=record["_time"], value=record["_value"])) # 수정 필요

    return data
