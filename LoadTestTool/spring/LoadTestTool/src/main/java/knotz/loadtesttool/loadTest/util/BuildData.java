package knotz.loadtesttool.loadTest.util;

import knotz.loadtesttool.loadTest.dto.ApiRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Map;

public class BuildData {

    private final FormatData formatData;

    public BuildData(FormatData formatData) {
        this.formatData = formatData;
    }

    // API 요청의 HTTP 메서드를 결정하는 메서드
    public HttpMethod determineHttpMethod(ApiRequest apiRequest) {
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
    public URI buildUri(ApiRequest apiRequest) {
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
    public HttpHeaders buildHeaders(ApiRequest apiRequest, String authCookie) {
        HttpHeaders headers = new HttpHeaders();

        // 사용자 정의 헤더 추가
        if (apiRequest.getHeaders() != null && !apiRequest.getHeaders().isEmpty()) {
            apiRequest.getHeaders().forEach(headers::add);
        }

        // Content-Type 설정
        if (apiRequest.getContentType() != null && !apiRequest.getContentType().isEmpty()) {
            headers.setContentType(apiRequest.contentTypeToMediaType());
        }

        // 인증 쿠키 추가
        if (authCookie != null && !authCookie.isEmpty()) {
            headers.add("Cookie", authCookie);
        }

        return headers;
    }


    // API 요청의 HTTP 엔티티를 빌드하는 메서드
    public HttpEntity<?> buildHttpEntity(ApiRequest apiRequest, HttpHeaders headers) {
        String contentType = apiRequest.getContentType();

        if ("application/json".equalsIgnoreCase(contentType)) {
            // JSON 본문 처리
            return new HttpEntity<>(apiRequest.getBody(), headers);
        } else if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
            // 폼 데이터 처리
            MultiValueMap<String, String> formData = formatData.parseFormData(apiRequest.getBody());
            return new HttpEntity<>(formData, headers);
        } else {
            // 기타 Content-Type 또는 본문 없음
            return new HttpEntity<>(apiRequest.getBody(), headers);
        }
    }
}
