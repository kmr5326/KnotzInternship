package knotz.loadtesttool.loadTest.util;

import knotz.loadtesttool.loadTest.dto.AllResult;
import knotz.loadtesttool.loadTest.dto.TestResult;
import knotz.loadtesttool.loadTest.dto.TestStatistics;

import java.util.List;

public class CalculateResult {

    private final FormatData formatData;

    public CalculateResult(FormatData formatData) {
        this.formatData = formatData;
    }

    public TestStatistics calculateAllResult(int durationSeconds, AllResult allResult, long overallStartTimeNano, List<TestResult> results) {
        double allThroughput = formatData.roundDouble((double) allResult.getSuccessfulRequests().get() / durationSeconds, 2);
        double allHitsPerSecond = formatData.roundDouble((double) allResult.getTotalRequests().get() / durationSeconds, 2);
        double allErrorsPerSecond = formatData.roundDouble((double) allResult.getFailedRequests().get() / durationSeconds, 2);
        double allTps = formatData.roundDouble((double) allResult.getSuccessfulRequests().get() / durationSeconds, 2);

        double allAvgResponseTime = allResult.getSuccessfulRequests().get() > 0
                ? formatData.roundDouble((double) allResult.getTotalResponseTimeNano().get() / allResult.getSuccessfulRequests().get() / 1_000_000.0, 2)
                : 0.0;
        double allAvgLatency = allResult.getSuccessfulRequests().get() > 0
                ? formatData.roundDouble((double) allResult.getTotalLatencyNano().get() / allResult.getSuccessfulRequests().get() / 1_000_000.0, 2)
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
        statistics.setTotalTimeSeconds(formatData.roundDouble(allTimeSeconds, 2)); // 전체 시간 설정
        return statistics;
    }

    public void calculateIndividualResult(double durationSeconds, List<TestResult> results) {
        for (TestResult testResult : results) {
            double throughput = formatData.roundDouble((double) testResult.getSuccessfulRequests().get() / durationSeconds, 2);
            double hitsPerSecond = formatData.roundDouble((double) testResult.getTotalRequests() / durationSeconds, 2);
            double errorsPerSecond = formatData.roundDouble((double) testResult.getFailedRequests().get() / durationSeconds, 2);
            double tps = formatData.roundDouble((double) testResult.getSuccessfulRequests().get() / durationSeconds, 2);

            // 평균 응답시간 및 지연시간 계산 (나노초 단위)
            double avgResponseTime = testResult.getSuccessfulRequests().get() > 0
                    ? formatData.roundDouble((double) testResult.getTotalResponseTimeNano().get() / testResult.getSuccessfulRequests().get(), 2)
                    : 0.0;
            double avgLatency = testResult.getSuccessfulRequests().get() > 0
                    ? formatData.roundDouble((double) testResult.getTotalLatencyNano().get() / testResult.getSuccessfulRequests().get(), 2)
                    : 0.0;

            testResult.setThroughput(throughput);
            testResult.setHitsPerSecond(hitsPerSecond);
            testResult.setErrorsPerSecond(errorsPerSecond);
            testResult.setTps(tps);
            testResult.setAverageResponseTime(avgResponseTime);
            testResult.setAverageLatency(avgLatency);
        }
    }

}
