package knotz.loadtesttool.loadTest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

// 결과 리스트를 담아서 보여줌
@Data
public class TestStatistics {
    @JsonProperty("개별 API 결과")
    private List<TestResult> results;

    @JsonProperty("전체 API 결과")
    private AllResult overallResult;

    @JsonProperty("총 시간(초)")
    private double totalTimeSeconds;
}
