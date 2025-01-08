package knotz.loadtesttool.loadTest.controller;

import knotz.loadtesttool.loadTest.dto.LoadTestRequest;
import knotz.loadtesttool.loadTest.service.LoadTestService;
import knotz.loadtesttool.loadTest.dto.TestStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/load-test")
@RequiredArgsConstructor
public class LoadTestController {

    private final LoadTestService loadTestService;

    @PostMapping("/run")
    public ResponseEntity<TestStatistics> executeLoadTest(
            @RequestBody LoadTestRequest loadTestRequest
    ) {
        // 입력값 검증
        if (loadTestRequest.getLoginRequest() == null ||
                loadTestRequest.getApiRequests() == null ||
                loadTestRequest.getApiRequests().isEmpty()) {
            log.warn("부하 테스트 요청이 잘못되었습니다: {}", loadTestRequest);
            return ResponseEntity.badRequest().body(null);
        }

        if (loadTestRequest.getNumberOfThreads() <= 0 ||
                loadTestRequest.getRampUpPeriodSeconds() < 0 ||
                loadTestRequest.getLoopCount() <= 0 ||
                loadTestRequest.getDurationSeconds() <= 0) {
            log.warn("스레드 수, 램프업 기간, 반복 횟수, 테스트 지속 시간이 유효하지 않습니다: {}", loadTestRequest);
            return ResponseEntity.badRequest().body(null);
        }

        try {
            log.info("부하 테스트 시작: {}", loadTestRequest);
            TestStatistics statistics = loadTestService.performLoadTest(
                    loadTestRequest.getApiRequests(),
                    loadTestRequest.getNumberOfThreads(),
                    loadTestRequest.getRampUpPeriodSeconds(),
                    loadTestRequest.getLoopCount(),
                    loadTestRequest.getDurationSeconds(),
                    loadTestRequest.getLoginRequest()
            );
            log.info("부하 테스트 완료: {}", statistics);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("부하 테스트 중 예외 발생", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}