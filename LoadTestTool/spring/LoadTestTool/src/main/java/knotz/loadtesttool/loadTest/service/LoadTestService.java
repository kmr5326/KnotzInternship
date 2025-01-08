package knotz.loadtesttool.loadTest.service;

import knotz.loadtesttool.loadTest.dto.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoadTestService {
    private final RestTemplate restTemplate;

    public LoadTestService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 소수점 이하를 두 자리까지 반올림하는 메서드.
     *
     * @param value 소수점 이하를 반올림할 값
     * @param places 소수점 이하 자리수
     * @return 반올림된 값
     */
    private double roundDouble(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * 부하 테스트를 수행하는 메서드.
     *
     * @param apiRequests         테스트할 API 리스트
     * @param numberOfThreads     사용할 스레드 수
     * @param rampUpPeriodSeconds 램프업 기간 (초)
     * @param loopCount           각 스레드당 반복 횟수
     * @param durationSeconds     테스트 지속 시간 (초)
     * @param loginRequest        로그인 요청 정보
     * @return 테스트 통계 정보
     * @throws InterruptedException 스레드가 인터럽트되었을 때 발생
     */
    public TestStatistics performLoadTest(List<ApiRequest> apiRequests,
                                          int numberOfThreads,
                                          int rampUpPeriodSeconds,
                                          int loopCount,
                                          int durationSeconds,
                                          LoginRequest loginRequest) throws InterruptedException {
        List<TestResult> results = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // 전체 테스트 시작 시점 기록
        Instant overallStart = Instant.now();

        // 로그인 요청을 먼저 수행하여 인증 쿠키를 획득
        String authCookie = performLogin(loginRequest);
        if (authCookie == null) {
            throw new RuntimeException("로그인에 실패했습니다.");
        }

        // Ramp-up 간격 계산 (밀리초 단위)
        long rampUpIntervalMillis = rampUpPeriodSeconds > 0 ? (rampUpPeriodSeconds * 1000L) / numberOfThreads : 0;

        // 전체 API에 대한 집계 변수 초기화
        AtomicInteger overallTotalRequests = new AtomicInteger(0);
        AtomicInteger overallSuccessfulRequests = new AtomicInteger(0);
        AtomicInteger overallFailedRequests = new AtomicInteger(0);
        AtomicLong overallTotalResponseTime = new AtomicLong(0);
        AtomicLong overallTotalLatency = new AtomicLong(0);

        // 테스트 종료 시간 계산
        long testEndTimeMillis = System.currentTimeMillis() + (durationSeconds * 1000L);

        for (ApiRequest apiRequest : apiRequests) {
            TestResult testResult = new TestResult();
            testResult.setApiName(apiRequest.getName());
            testResult.setTotalRequests(numberOfThreads * loopCount);
            testResult.setSuccessfulRequests(0);
            testResult.setFailedRequests(0);

            // AtomicLong을 사용하여 스레드 안전하게 누적
            AtomicLong totalResponseTime = new AtomicLong(0);
            AtomicLong totalLatency = new AtomicLong(0);

            for (int i = 0; i < numberOfThreads; i++) {
                final int threadIndex = i;
                executorService.submit(() -> {
                    try {
                        // Ramp-up 간격 대기
                        if (rampUpIntervalMillis > 0) {
                            Thread.sleep(threadIndex * rampUpIntervalMillis);
                        }

                        for (int j = 0; j < loopCount; j++) {
                            // 테스트 지속 시간 체크
                            if (System.currentTimeMillis() > testEndTimeMillis) {
                                break;
                            }

                            Instant requestSentTime = Instant.now();

                            // 빌드 URL with Path Variables and Query Parameters
                            URI uri = buildUri(apiRequest);

                            // 빌드 Headers (인증 쿠키 포함)
                            HttpHeaders headers = buildHeaders(apiRequest, authCookie);

                            // 빌드 Body
                            HttpEntity<?> entity = buildHttpEntity(apiRequest, headers);

                            // 요청 메서드 결정
                            HttpMethod httpMethod = determineHttpMethod(apiRequest);

                            try {
                                Instant responseStartTime = Instant.now();
                                ResponseEntity<String> response = restTemplate.exchange(uri, httpMethod, entity, String.class);
                                Instant responseReceivedTime = Instant.now();

                                // 지연 시간 및 응답 시간 계산
                                long latency = Duration.between(requestSentTime, responseStartTime).toMillis();
                                long responseTime = Duration.between(requestSentTime, responseReceivedTime).toMillis();

                                // AtomicLong에 누적
                                totalLatency.addAndGet(latency);
                                totalResponseTime.addAndGet(responseTime);

                                overallTotalRequests.incrementAndGet();

                                if (response.getStatusCode().is2xxSuccessful()) {
                                    testResult.setSuccessfulRequests(testResult.getSuccessfulRequests() + 1);
                                    overallSuccessfulRequests.incrementAndGet();
                                } else {
                                    testResult.setFailedRequests(testResult.getFailedRequests() + 1);
                                    overallFailedRequests.incrementAndGet();
                                }

                            } catch (Exception e) {
                                testResult.setFailedRequests(testResult.getFailedRequests() + 1);
                                overallFailedRequests.incrementAndGet();
                                // 예외 로그 추가 (선택 사항)
                                System.err.println("API 요청 중 예외 발생: " + e.getMessage());
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("스레드 인터럽트 발생: " + e.getMessage());
                    }
                });
            }

            // 테스트 결과 수집을 위한 동기화 지점 설정
            results.add(testResult);
        }

        // 테스트 지속 시간 동안 대기
        long remainingTimeMillis = testEndTimeMillis - System.currentTimeMillis();
        if (remainingTimeMillis > 0) {
            Thread.sleep(remainingTimeMillis);
        }

        // 모든 스레드가 완료될 때까지 대기
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(durationSeconds, TimeUnit.SECONDS);
        if (!terminated) {
            executorService.shutdownNow();
        }

        // 개별 API 결과 메트릭 계산
        for (TestResult testResult : results) {
            double totalTimeSeconds = (double) durationSeconds;

            double throughput = roundDouble((double) testResult.getSuccessfulRequests() / totalTimeSeconds, 2);
            double hitsPerSecond = roundDouble((double) testResult.getTotalRequests() / totalTimeSeconds, 2);
            double errorsPerSecond = roundDouble((double) testResult.getFailedRequests() / totalTimeSeconds, 2);
            double tps = roundDouble((double) testResult.getSuccessfulRequests() / totalTimeSeconds, 2);

            double avgResponseTime = testResult.getSuccessfulRequests() > 0
                    ? roundDouble((double) testResult.getAverageResponseTime() / testResult.getSuccessfulRequests(), 2)
                    : 0.0;
            double avgLatency = testResult.getSuccessfulRequests() > 0
                    ? roundDouble((double) testResult.getAverageLatency() / testResult.getSuccessfulRequests(), 2)
                    : 0.0;

            testResult.setThroughput(throughput);
            testResult.setHitsPerSecond(hitsPerSecond);
            testResult.setErrorsPerSecond(errorsPerSecond);
            testResult.setTps(tps);
            testResult.setAverageResponseTime(avgResponseTime);
            testResult.setAverageLatency(avgLatency);
        }

        // 전체 API 집계 결과 계산
        AllResult allResult = new AllResult();
        allResult.setTotalRequests(overallTotalRequests.get());
        allResult.setSuccessfulRequests(overallSuccessfulRequests.get());
        allResult.setFailedRequests(overallFailedRequests.get());

        double overallThroughput = roundDouble((double) overallSuccessfulRequests.get() / durationSeconds, 2);
        double overallHitsPerSecond = roundDouble((double) overallTotalRequests.get() / durationSeconds, 2);
        double overallErrorsPerSecond = roundDouble((double) overallFailedRequests.get() / durationSeconds, 2);
        double overallTps = roundDouble((double) overallSuccessfulRequests.get() / durationSeconds, 2);

        double overallAvgResponseTime = overallSuccessfulRequests.get() > 0
                ? roundDouble((double) overallTotalResponseTime.get() / overallSuccessfulRequests.get(), 2)
                : 0.0;
        double overallAvgLatency = overallSuccessfulRequests.get() > 0
                ? roundDouble((double) overallTotalLatency.get() / overallSuccessfulRequests.get(), 2)
                : 0.0;

        allResult.setThroughput(overallThroughput);
        allResult.setHitsPerSecond(overallHitsPerSecond);
        allResult.setErrorsPerSecond(overallErrorsPerSecond);
        allResult.setTps(overallTps);
        allResult.setAverageResponseTime(overallAvgResponseTime);
        allResult.setAverageLatency(overallAvgLatency);

        // 전체 테스트 종료 시점 기록
        Instant overallEnd = Instant.now();
        double overallTimeSeconds = Duration.between(overallStart, overallEnd).toMillis() / 1000.0;

        TestStatistics statistics = new TestStatistics();
        statistics.setResults(results);
        statistics.setOverallResult(allResult);
        statistics.setTotalTimeSeconds(roundDouble(overallTimeSeconds, 2)); // 전체 시간 설정
        return statistics;
    }

    /**
     * 로그인 요청을 수행하고 인증 쿠키를 반환하는 메서드.
     *
     * @param loginRequest 로그인 요청 정보
     * @return 인증 쿠키 값
     */
    private String performLogin(LoginRequest loginRequest) {
        // 로그인 요청 URL
        String loginUrl = "http://61.37.80.126:5200/api/auth/cookie/localadmin"; // 실제 URL로 변경

        // 헤더 설정: Content-Type을 multipart/form-data로 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 폼 데이터 설정
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("username", loginRequest.getUsername());
        formData.add("password", loginRequest.getPassword());

        // HTTP 엔티티 생성
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(formData, headers);

        try {
            // 로그인 요청 수행
            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                    loginUrl,
                    HttpMethod.POST,
                    entity,
                    LoginResponse.class
            );

            // 성공적인 로그인 응답인지 확인
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 'data' 필드에서 쿠키 값 추출
                return response.getBody().getData();
            } else {
                System.err.println("로그인 실패: " + response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("로그인 중 예외 발생: " + e.getMessage());
            e.printStackTrace(); // 예외 스택 트레이스 출력
            return null;
        }
    }

    /**
     * API 요청의 HTTP 메서드를 결정하는 메서드.
     *
     * @param apiRequest API 요청 정보
     * @return 결정된 HttpMethod 객체
     */
    private HttpMethod determineHttpMethod(ApiRequest apiRequest) {
        String methodStr = apiRequest.getMethod();
        System.out.println("API Request method: " + methodStr); // 디버깅용 로그

        // 메서드 값이 null이거나 빈 문자열인지 확인
        if (methodStr == null || methodStr.trim().isEmpty()) {
            throw new IllegalArgumentException("HTTP method is null or empty for API: " + apiRequest.getName());
        }

        try {
            return HttpMethod.valueOf(methodStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + methodStr + " for API: " + apiRequest.getName());
        }
    }

    /**
     * API 요청의 URI를 빌드하는 메서드.
     *
     * @param apiRequest API 요청 정보
     * @return 빌드된 URI
     */
    private URI buildUri(ApiRequest apiRequest) {
        String url = apiRequest.getUrl();

        // 경로 변수 처리
        if (apiRequest.getPathVariables() != null && !apiRequest.getPathVariables().isEmpty()) {
            for (Map.Entry<String, String> entry : apiRequest.getPathVariables().entrySet()) {
                url = url.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        // 쿼리 파라미터 처리
        if (apiRequest.getQueryParameters() != null && !apiRequest.getQueryParameters().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("?");
            apiRequest.getQueryParameters().forEach((key, value) -> {
                sb.append(key).append("=").append(value).append("&");
            });
            // 마지막 '&' 제거
            sb.setLength(sb.length() - 1);
            url += sb.toString();
        }

        return URI.create(url);
    }

    /**
     * API 요청의 헤더를 빌드하는 메서드. 인증 쿠키를 추가함.
     *
     * @param apiRequest API 요청 정보
     * @param authCookie 인증 쿠키 값
     * @return 빌드된 HttpHeaders 객체
     */
    private HttpHeaders buildHeaders(ApiRequest apiRequest, String authCookie) {
        HttpHeaders headers = new HttpHeaders();

        // 사용자 정의 헤더 추가
        if (apiRequest.getHeaders() != null && !apiRequest.getHeaders().isEmpty()) {
            apiRequest.getHeaders().forEach(headers::add);
        }

        // Content-Type 설정
        if (apiRequest.getContentType() != null && !apiRequest.getContentType().isEmpty()) {
            headers.setContentType(MediaType.parseMediaType(apiRequest.getContentType()));
        }

        // 인증 쿠키 추가
        if (authCookie != null && !authCookie.isEmpty()) {
            headers.add("Cookie", authCookie);
        }

        return headers;
    }

    /**
     * API 요청의 HTTP 엔티티를 빌드하는 메서드.
     *
     * @param apiRequest API 요청 정보
     * @param headers    헤더 정보
     * @return 빌드된 HttpEntity 객체
     */
    private HttpEntity<?> buildHttpEntity(ApiRequest apiRequest, HttpHeaders headers) {
        String contentType = apiRequest.getContentType();

        if ("application/json".equalsIgnoreCase(contentType)) {
            // JSON 본문 처리
            return new HttpEntity<>(apiRequest.getBody(), headers);
        } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
            // 폼 데이터 처리
            MultiValueMap<String, String> formData = parseFormData(apiRequest.getBody());
            return new HttpEntity<>(formData, headers);
        } else {
            // 기타 Content-Type 또는 본문 없음
            return new HttpEntity<>(apiRequest.getBody(), headers);
        }
    }

    /**
     * 폼 데이터 문자열을 MultiValueMap으로 변환하는 메서드.
     *
     * @param body 폼 데이터 문자열 (예: "key1=value1&key2=value2")
     * @return 변환된 MultiValueMap 객체
     */
    private MultiValueMap<String, String> parseFormData(String body) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (body != null && !body.isEmpty()) {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    formData.add(keyValue[0], keyValue[1]);
                } else if (keyValue.length == 1) {
                    formData.add(keyValue[0], "");
                }
            }
        }
        return formData;
    }
}