package knotz.loadtesttool.loadTest.service;

import knotz.loadtesttool.loadTest.dto.*;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class LoadTestService {
    private final RestTemplate restTemplate;

    public LoadTestService() {
        this.restTemplate = new RestTemplate();
    }


    // 소수점 이하를 두 자리까지 반올림하는 메서드
    private double roundDouble(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
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
        long overallStartTimeNano = System.nanoTime();

        // 로그인 요청을 먼저 수행하여 인증 쿠키를 획득
        String authCookie = performLogin(loginRequest);
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

                            // 요청 시작 시간 측정 (나노초 단위)
                            long requestSentTimeNano = System.nanoTime();

                            // 빌드 URL with Path Variables and Query Parameters
                            URI uri = buildUri(apiRequest);

                            // 빌드 Headers (인증 쿠키 포함)
                            HttpHeaders headers = buildHeaders(apiRequest, authCookie);

                            // 빌드 Body
                            HttpEntity<?> entity = buildHttpEntity(apiRequest, headers);

                            // 요청 메서드 결정
                            HttpMethod httpMethod = determineHttpMethod(apiRequest);

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

                                // 요청 수 업데이트
                                allResult.setTotalRequests(allResult.getTotalRequests() + 1);

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
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.err.println("스레드 인터럽트 발생: " + e.getMessage());
                    }
                });
            }
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

        // 개별 API 결과 메트릭 계산 (나노초 단위)
        for (TestResult testResult : results) {
            double throughput = roundDouble((double) testResult.getSuccessfulRequests().get() / (double) durationSeconds, 2);
            double hitsPerSecond = roundDouble((double) testResult.getTotalRequests() / (double) durationSeconds, 2);
            double errorsPerSecond = roundDouble((double) testResult.getFailedRequests().get() / (double) durationSeconds, 2);
            double tps = roundDouble((double) testResult.getSuccessfulRequests().get() / (double) durationSeconds, 2);

            // 평균 응답시간 및 지연시간 계산 (나노초 단위)
            double avgResponseTime = testResult.getSuccessfulRequests().get() > 0
                    ? roundDouble((double) testResult.getTotalResponseTimeNano().get() / testResult.getSuccessfulRequests().get(), 2)
                    : 0.0;
            double avgLatency = testResult.getSuccessfulRequests().get() > 0
                    ? roundDouble((double) testResult.getTotalLatencyNano().get() / testResult.getSuccessfulRequests().get(), 2)
                    : 0.0;

            testResult.setThroughput(throughput);
            testResult.setHitsPerSecond(hitsPerSecond);
            testResult.setErrorsPerSecond(errorsPerSecond);
            testResult.setTps(tps);
            testResult.setAverageResponseTime(avgResponseTime);
            testResult.setAverageLatency(avgLatency);
        }

        // 전체 API 집계 결과 계산 (밀리초 단위)
        double allThroughput = roundDouble((double) allResult.getSuccessfulRequests().get() / durationSeconds, 2);
        double allHitsPerSecond = roundDouble((double) allResult.getTotalRequests() / durationSeconds, 2);
        double allErrorsPerSecond = roundDouble((double) allResult.getFailedRequests().get() / durationSeconds, 2);
        double allTps = roundDouble((double) allResult.getSuccessfulRequests().get() / durationSeconds, 2);

        double allAvgResponseTime = allResult.getSuccessfulRequests().get() > 0
                ? roundDouble((double) allResult.getTotalResponseTimeNano().get() / allResult.getSuccessfulRequests().get() / 1_000_000.0, 2)
                : 0.0;
        double allAvgLatency = allResult.getSuccessfulRequests().get() > 0
                ? roundDouble((double) allResult.getTotalLatencyNano().get() / allResult.getSuccessfulRequests().get() / 1_000_000.0, 2)
                : 0.0;

        allResult.setThroughput(allThroughput);
        allResult.setHitsPerSecond(allHitsPerSecond);
        allResult.setErrorsPerSecond(allErrorsPerSecond);
        allResult.setTps(allTps);
        allResult.setAverageResponseTime(allAvgResponseTime);
        allResult.setAverageLatency(allAvgLatency);

        // 전체 테스트 종료 시점 기록 (나노초 단위)
        long allEndTimeNano = System.nanoTime();
        double allTimeSeconds = (allEndTimeNano - overallStartTimeNano) / 1_000_000_000.0;

        TestStatistics statistics = new TestStatistics();
        statistics.setResults(results);
        statistics.setOverallResult(allResult);
        statistics.setTotalTimeSeconds(roundDouble(allTimeSeconds, 2)); // 전체 시간 설정
        return statistics;
    }


    // 로그인 요청을 수행하고 인증 쿠키를 반환하는 메서드
    private String performLogin(LoginRequest loginRequest) {
        // 로그인 요청 URL
        String loginUrl = "http://61.37.80.126:5200/api/auth/cookie/";

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
                    loginUrl + loginRequest.getUsername(),
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


    // API 요청의 HTTP 메서드를 결정하는 메서드
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


    // API 요청의 URI를 빌드하는 메서드
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


    // API 요청의 헤더를 빌드하는 메서드, 인증 쿠키를 추가함
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


    // API 요청의 HTTP 엔티티를 빌드하는 메서드
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

    // 폼 데이터 문자열을 MultiValueMap으로 변환하는 메서드
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