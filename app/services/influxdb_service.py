from typing import List
from influxdb_client import InfluxDBClient
from app.models import InfluxData



# InfluxDB 연결 설정
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "kX_bdeBq1rSYvTAkakBgNb-Qu30byyf1_ceyG60djgh2jneoRRXuUSlWU77lLf8CqEFobYwNUp54PAvZ1J5MiA=="
INFLUXDB_ORG = "knotz"
INFLUXDB_BUCKET = "bucket1"

client = InfluxDBClient(url=INFLUXDB_URL, token=INFLUXDB_TOKEN, org=INFLUXDB_ORG)
query_api = client.query_api()


def get_influxdb_data() -> List[InfluxData]:
    query = f'''
    from(bucket: "{INFLUXDB_BUCKET}")
        |> range(start: -1h)  # 최근 1시간의 데이터 가져오기
        |> filter(fn: (r) => r["_measurement"] == "your_measurement")
    '''
    result = query_api.query(query)

    data = []
    for table in result:
        for record in table.records:
            data.append(InfluxData(time=record["_time"], value=record["_value"]))

    return data
