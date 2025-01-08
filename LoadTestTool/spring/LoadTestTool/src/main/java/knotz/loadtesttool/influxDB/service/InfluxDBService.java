package knotz.loadtesttool.influxDB.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class InfluxDBService {

    @Autowired
    private InfluxDBClient influxDBClient;

    public void writeData() {
//        // 테스트용 데이터를 생성
//        Point row = Point.measurement("launcher_client_connection")
//                .addTag("privateIp", "127.0.0.1")
//                .addTag("port", "8086")
//                .addField("clientCnt", 10);
//
//        // InfluxDB에 데이터 기록
//        influxDBClient.getWriteApiBlocking().writePoint(row);

        for (int i = 0; i < 100; i++) { // 5번 반복 예시
            Point row = Point.measurement("launcher_client_connection")
                    .addTag("privateIp", "192.168.1.100")
                    .addTag("port", "8080")
                    .addField("clientCnt", (int) (Math.random() * 100)); // clientCnt 값을 반복문에서 변경

            // InfluxDB에 데이터 기록
            influxDBClient.getWriteApiBlocking().writePoint(row);
        }
    }
}