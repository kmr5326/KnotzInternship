package knotz.loadtesttool.loadTest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

// json으로 테스트 결과를 보여줌
@Data
public class TestResult {
    @JsonProperty("API 이름")
    private String apiName;

    @JsonProperty("총 요청 수")
    private int totalRequests;

    @JsonProperty("성공 요청 수")
    private AtomicInteger successfulRequests = new AtomicInteger(0);

    @JsonProperty("실패 요청 수")
    private AtomicInteger failedRequests = new AtomicInteger(0);

    @JsonProperty("처리량")
    private double throughput;

    @JsonProperty("평균 응답시간(ns)")
    private double averageResponseTime;

    @JsonProperty("평균 지연시간(ns)")
    private double averageLatency;

    @JsonProperty("초당 히트 수")
    private double hitsPerSecond;

    @JsonProperty("초당 트랜잭션 수")
    private double tps;

    @JsonProperty("초당 오류 수")
    private double errorsPerSecond;

    @JsonIgnore // 총 응답 시간(ns)
    private AtomicLong totalResponseTimeNano = new AtomicLong(0);

    @JsonIgnore // 총 지연 시간(ns)
    private AtomicLong totalLatencyNano = new AtomicLong(0);
}
