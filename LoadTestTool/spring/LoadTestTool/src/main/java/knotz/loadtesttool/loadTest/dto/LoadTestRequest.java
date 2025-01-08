package knotz.loadtesttool.loadTest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class LoadTestRequest {
    @JsonProperty("로그인 요청")
    private LoginRequest loginRequest;

    @JsonProperty("API 요청 리스트")
    private List<ApiRequest> apiRequests;

    @JsonProperty("스레드 수")
    private int numberOfThreads;

    @JsonProperty("램프업 기간(초)")
    private int rampUpPeriodSeconds;

    @JsonProperty("반복횟수")
    private int loopCount;

    @JsonProperty("테스트 지속시간(초)")
    private int durationSeconds;
}
