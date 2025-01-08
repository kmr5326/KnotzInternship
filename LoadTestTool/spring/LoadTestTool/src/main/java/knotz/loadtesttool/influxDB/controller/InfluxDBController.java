package knotz.loadtesttool.influxDB.controller;

import knotz.loadtesttool.influxDB.service.InfluxDBService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/influxDB")
public class InfluxDBController {


    private final InfluxDBService influxDBService;
    private boolean isWriting = false;

    // 버튼 클릭 시 데이터를 InfluxDB에 기록
    @GetMapping("/write-to-influxdb")
    public String writeToInfluxDB() {
//        influxDBService.writeData();
        if (!isWriting) {
            isWriting = true;
            new Thread(() -> {
                // 5초 동안 데이터 보내기
                for (int i = 0; i < 5; i++) {
                    influxDBService.writeData();  // 데이터 기록
                    try {
                        Thread.sleep(1000);  // 1초 간격으로 데이터 보내기
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                isWriting = false;
            }).start();
        }
        return "redirect:/";
    }
}
