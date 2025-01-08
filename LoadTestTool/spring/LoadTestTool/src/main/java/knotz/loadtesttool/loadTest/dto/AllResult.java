package knotz.loadtesttool.loadTest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AllResult {
    @JsonProperty("총요청수")
    private int totalRequests;

    @JsonProperty("성공요청수")
    private int successfulRequests;

    @JsonProperty("실패요청수")
    private int failedRequests;

    @JsonProperty("처리량")
    private double throughput;

    @JsonProperty("평균응답시간")
    private double averageResponseTime;

    @JsonProperty("평균지연시간")
    private double averageLatency;

    @JsonProperty("초당히트수")
    private double hitsPerSecond;

    @JsonProperty("초당트랜잭션수")
    private double tps;

    @JsonProperty("초당오류수")
    private double errorsPerSecond;
}
