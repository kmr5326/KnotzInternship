package knotz.loadtesttool.influxDB.dto;

import knotz.loadtesttool.loadTest.dto.ApiRequest;
import knotz.loadtesttool.loadTest.dto.TestResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiLoadTestResult {
    // API 이름
    private String apiName;

    // 요청 상태 (성공/실패)
    private boolean status;

    // 총 요청 수
    private int totalRequests;

    // 성공한 요청 수
    private int successfulRequests;

    // 실패한 요청 수
    private int failedRequests;

    // 지연 시간 (나노초)
    private long latencyNs;

    // 응답 시간 (나노초)
    private long responseTimeNs;

    // 초당 처리량
    private double throughput;

    // 초당 히트 수
    private double hitRate;

    // 초당 트랜잭션 수
    private double transactionRate;

    // 초당 오류 수
    private double errorRate;

    // 타임스탬프 (밀리초)
    private long timestamp;

//    private ApiLoadTestResult buildApiLoadTestResult(ApiRequest apiRequest, TestResult testResult,boolean responseStatus){
//        return ApiLoadTestResult.builder()
//                .apiName(apiRequest.getName())
//                .status(responseStatus)
//                .totalRequests(testResult.getTotalRequests())
//                .successfulRequests()
//                .build();
//    }
}
