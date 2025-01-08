package knotz.loadtesttool.loadTest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

// 요청하는 API의 정보
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiRequest {
    private String name; // API 이름

    private String url;  // 요청 URL

    private String method; // HTTP 메서드 (GET, POST, PUT, DELETE 등)

    private Map<String, String> pathVariables; // 경로 변수

    private Map<String, String> queryParameters; // 쿼리 파라미터

    private Map<String, String> headers; // 요청 헤더

    private String body; // 요청 본문 (JSON, 폼 데이터 등)

    private String contentType; // Content-Type (application/json, application/x-www-form-urlencoded 등)
}
