package knotz.loadtesttool.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import knotz.loadtesttool.influxDB.service.InfluxDBService;
import knotz.loadtesttool.loadTest.dto.LoadTestRequest;
import knotz.loadtesttool.loadTest.dto.TestStatistics;
import knotz.loadtesttool.loadTest.service.LoadTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class WebController {
    private final LoadTestService loadTestService;
    private final InfluxDBService influxDBService;
    private final ResourceLoader resourceLoader;

    @GetMapping("")
    public String index(Model model) {
        ObjectMapper objectMapper = new ObjectMapper();

        List<String> apis = new ArrayList<>();
        Map<String, List<String>> apiParameters = null;

        try {
            Resource resource = resourceLoader.getResource("classpath:api_config.json");
            apiParameters = objectMapper.readValue(resource.getInputStream(), Map.class);
            apis = List.copyOf(apiParameters.keySet());
        } catch (IOException e) {
            log.error("api-config.json File read Error : {}", e.getMessage());
            // 파일 읽기 오류 처리
        }

        try {
            String apiParametersJson = objectMapper.writeValueAsString(apiParameters);
            model.addAttribute("apiParameters", apiParametersJson);
        } catch (JsonProcessingException e) {
            log.error("apiParameters Json parsing Error: {}", e.getMessage());
        }

        model.addAttribute("APIs", apis);
        return "index";
    }

    @PostMapping("/submit")
    public String submit(@ModelAttribute LoadTestRequest loadTestRequest, Model model) {
        log.info("부하 테스트 요청 수신: {}", loadTestRequest);

        try {
            // 부하 테스트 실행
            TestStatistics statistics = loadTestService.performLoadTest(
                    loadTestRequest.getApiRequests(),
                    loadTestRequest.getNumberOfThreads(),
                    loadTestRequest.getRampUpPeriodSeconds(),
                    loadTestRequest.getLoopCount(),
                    loadTestRequest.getDurationSeconds(),
                    loadTestRequest.getLoginRequest()
            );

            // 테스트 결과를 모델에 추가
            model.addAttribute("statistics", statistics);
            log.info("부하 테스트 결과{}", statistics);
            return "result"; // 결과를 표시할 뷰 이름

        } catch (Exception e) {
            log.error("부하 테스트 수행 중 예외 발생: {}", e.getMessage());
            model.addAttribute("errorMessage", "부하 테스트 수행 중 오류가 발생했습니다.");
            return "error"; // 오류를 표시할 뷰 이름
        }
    }
}
