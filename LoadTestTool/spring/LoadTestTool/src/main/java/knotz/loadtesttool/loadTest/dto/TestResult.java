package knotz.loadtesttool.loadTest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

// json으로 테스트 결과를 보여줌
@Data
public class TestResult {
    @JsonProperty("API 이름")
    private String apiName;

    @JsonProperty("총 요청수")
    private int totalRequests;

    @JsonProperty("성공 요청수")
    private int successfulRequests;

    @JsonProperty("실패 요청수")
    private int failedRequests;

    @JsonProperty("처리량")
    private double throughput;

    @JsonProperty("평균 응답시간")
    private double averageResponseTime;

    @JsonProperty("평균 지연시간")
    private double averageLatency;

    @JsonProperty("초당 히트 수")
    private double hitsPerSecond;

    @JsonProperty("초당 트랜잭션 수")
    private double tps;

    @JsonProperty("초당 오류 수")
    private double errorsPerSecond;
}
