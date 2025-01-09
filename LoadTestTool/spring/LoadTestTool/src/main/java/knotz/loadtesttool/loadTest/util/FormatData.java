package knotz.loadtesttool.loadTest.util;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class FormatData {

    // 소수점 이하를 두 자리까지 반올림하는 메서드
    public double roundDouble(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    // 폼 데이터 문자열을 MultiValueMap으로 변환하는 메서드
    public MultiValueMap<String, String> parseFormData(String body) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (body != null && !body.isEmpty()) {
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    formData.add(keyValue[0], keyValue[1]);
                } else if (keyValue.length == 1) {
                    formData.add(keyValue[0], "");
                }
            }
        }
        return formData;
    }
}
