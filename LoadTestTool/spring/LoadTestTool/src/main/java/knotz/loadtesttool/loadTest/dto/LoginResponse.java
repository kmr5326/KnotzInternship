package knotz.loadtesttool.loadTest.dto;

import lombok.Data;

import java.util.List;

// 로그인 응답 시 JSON 정보 받아옴
@Data
public class LoginResponse {
    private User user; // 사용자 정보
    private String data; // 쿠키 값


    // 사용자 정보를 나타내는 내부 클래스
    @Data
    public static class User {
        private String id;
        private String userName;
        private String email;
        private boolean emailConfirmed;
        private List<Claim> claims;
    }


    // 클레임 정보를 나타내는 내부 클래스
    @Data
    public static class Claim {
        private int id;
        private String claimType;
        private String claimValue;
    }
}
