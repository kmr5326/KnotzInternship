package knotz.loadtesttool.loadTest.service;

import knotz.loadtesttool.loadTest.dto.LoginRequest;
import knotz.loadtesttool.loadTest.dto.LoginResponse;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class LoginService {

    // 로그인 요청을 수행하고 인증 쿠키를 반환하는 메서드
    public String performLogin(LoginRequest loginRequest, RestTemplate restTemplate) {
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
}
