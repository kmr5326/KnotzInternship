package knotz.loadtesttool.loadTest.service;

import knotz.loadtesttool.influxDB.service.InfluxDBService;
import knotz.loadtesttool.loadTest.dto.*;
import knotz.loadtesttool.loadTest.util.BuildData;
import knotz.loadtesttool.loadTest.util.CalculateResult;
import knotz.loadtesttool.loadTest.util.FormatData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Service
public class LoadTestService {
    private final RestTemplate restTemplate;
    private final FormatData formatData;
    private final BuildData buildData;
    private final CalculateResult calculateResult;
    private final InfluxDBService influxDBService;
    private final LoginService loginService;

    @Autowired
    public LoadTestService(InfluxDBService influxDBService) {
        this.restTemplate = new RestTemplate();
        this.formatData = new FormatData();
        this.buildData = new BuildData(formatData);
        this.calculateResult = new CalculateResult(formatData);
        this.influxDBService= influxDBService;
        this.loginService = new LoginService();
    }

    // 부하 테스트를 수행하는 메서드
    public TestStatistics performLoadTest(List<ApiRequest> apiRequests,
                                          int numberOfThreads,
                                          int rampUpPeriodSeconds,
                                          int loopCount,
                                          int durationSeconds,
                                          LoginRequest loginRequest) throws InterruptedException {
        List<TestResult> results = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 전체 테스트 시작 시점 기록 (나노초 단위)
        long allStartTimeNano = System.nanoTime();

        // 로그인 요청을 먼저 수행하여 인증 쿠키를 획득
        String authCookie = loginService.performLogin(loginRequest, restTemplate);
        if (authCookie == null) {
            throw new RuntimeException("로그인에 실패했습니다.");
        }

        // Ramp-up 간격 계산 (밀리초 단위)
        long rampUpIntervalMillis = rampUpPeriodSeconds > 0 ? (rampUpPeriodSeconds * 1000L) / numberOfThreads : 0;

        // 전체 API에 대한 집계 변수 초기화
        AllResult allResult = new AllResult();

        // 테스트 종료 시간 계산
        long testEndTimeMillis = System.currentTimeMillis() + (durationSeconds * 1000L);

        for (ApiRequest apiRequest : apiRequests) {
            TestResult testResult = new TestResult();
            testResult.setApiName(apiRequest.getName());
            testResult.setTotalRequests(numberOfThreads * loopCount);
            // successfulRequests, failedRequests는 AtomicInteger로 초기화되어 있으므로 별도로 설정할 필요 없음

            results.add(testResult);

            // 스레드 생성 및 실행
            createAndRunThread(numberOfThreads, loopCount, apiRequest, executorService, rampUpIntervalMillis, testEndTimeMillis, authCookie, testResult, allResult);
        }

        // 테스트 지속 시간 동안 대기
        long remainingTimeMillis = testEndTimeMillis - System.currentTimeMillis();
        if (remainingTimeMillis > 0) {
            Thread.sleep(remainingTimeMillis);
        }

        // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(remainingTimeMillis + (loopCount * 1000L), TimeUnit.MILLISECONDS);
        if (!terminated) {
            executorService.shutdownNow();
        }

        // 개별 API 결과 계산 (나노초 단위)
        calculateResult.calculateIndividualResult(durationSeconds, results);

        // 전체 API 집계 결과 계산 (밀리초 단위)
        return calculateResult.calculateAllResult(durationSeconds, allResult, allStartTimeNano, results);
    }

    private void createAndRunThread(int numberOfThreads, int loopCount, ApiRequest apiRequest, ExecutorService executorService, long rampUpIntervalMillis, long testEndTimeMillis, String authCookie, TestResult testResult, AllResult allResult) {
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            executorService.submit(() -> {
                try {
                    // Ramp-up 간격 대기
                    if (rampUpIntervalMillis > 0) {
                        Thread.sleep(threadIndex * rampUpIntervalMillis);
                    }

                    // 각 스레드 요청 반복
                    loopThread(loopCount, apiRequest, testEndTimeMillis, authCookie, testResult, allResult);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("스레드 인터럽트 발생: " + e.getMessage());
                }
            });
        }
    }

    private void loopThread(int loopCount, ApiRequest apiRequest, long testEndTimeMillis, String authCookie, TestResult testResult, AllResult allResult) {
        for (int j = 0; j < loopCount; j++) {
            // 테스트 지속 시간 체크
            if (System.currentTimeMillis() > testEndTimeMillis) {
                break;
            }

            // 요청 시작 시간 측정 (나노초 단위)
            long requestSentTimeNano = System.nanoTime();

            // 빌드 URL with Path Variables and Query Parameters
            URI uri = buildData.buildUri(apiRequest);

            // 빌드 Headers (인증 쿠키 포함)
            HttpHeaders headers = buildData.buildHeaders(apiRequest, authCookie);

            // 빌드 Body
            HttpEntity<?> entity = buildData.buildHttpEntity(apiRequest, headers);

            // 요청 메서드 결정
            HttpMethod httpMethod = buildData.determineHttpMethod(apiRequest);

            try {
                // API 요청 수행 및 응답 시간 측정
                long responseStartTimeNano = System.nanoTime();
                ResponseEntity<String> response = restTemplate.exchange(uri, httpMethod, entity, String.class);
                long responseEndTimeNano = System.nanoTime();

                // 지연 시간 및 응답 시간 계산
                long latencyNano = responseStartTimeNano - requestSentTimeNano;
                long responseTimeNano = responseEndTimeNano - requestSentTimeNano;

                // 누적 (AtomicLong 사용으로 스레드 안전성 보장)
                testResult.getTotalLatencyNano().addAndGet(latencyNano);
                testResult.getTotalResponseTimeNano().addAndGet(responseTimeNano);

                allResult.getTotalLatencyNano().addAndGet(latencyNano);
                allResult.getTotalResponseTimeNano().addAndGet(responseTimeNano);

                influxDBService.saveLoadTestResult(apiRequest.getName(), latencyNano,responseTimeNano,response.getStatusCode().is2xxSuccessful());

                // 요청 수 업데이트 (AtomicInteger 사용)
                allResult.getTotalRequests().incrementAndGet();

                if (response.getStatusCode().is2xxSuccessful()) {
                    testResult.getSuccessfulRequests().incrementAndGet();
                    allResult.getSuccessfulRequests().incrementAndGet();
                } else {
                    testResult.getFailedRequests().incrementAndGet();
                    allResult.getFailedRequests().incrementAndGet();
                }

            } catch (Exception e) {
                testResult.getFailedRequests().incrementAndGet();
                allResult.getFailedRequests().incrementAndGet();
                // 예외 로그 추가 (선택 사항)
                System.err.println("API 요청 중 예외 발생: " + e.getMessage());
            }
        }
    }
}