package knotz.loadtesttool.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class LoadTestController {

    @GetMapping("")
    public String index(Model model) {
        List<String> apis = Arrays.asList("AuthCookieAPI", "AuthLogoutAPI", "UserGetAllAPI");

        Map<String, List<String>> apiParameters = new HashMap<>();
        apiParameters.put("AuthCookieAPI", Arrays.asList("username", "password"));
        apiParameters.put("UserGetAllAPI", Arrays.asList("name", "pagesize", "page"));
        apiParameters.put("AuthLogoutAPI", Arrays.asList("username"));

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String apiParametersJson = objectMapper.writeValueAsString(apiParameters);
            model.addAttribute("apiParameters", apiParametersJson);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        model.addAttribute("APIs", apis);
//        model.addAttribute("apiParameters", apiParameters);
        return "index";
    }

//    @PostMapping("submit")
//    public String submit(@RequestParam Map<String, String> params){

    /// /        String selectedApi = params.get("APIs");
    /// /        log.info(selectedApi);
//
//        params.forEach((key, value) -> {
//            log.info("Parameter Name: {}, Value: {}", key, value);
//        });
//        return "redirect:/";
//    }
    @PostMapping("/submit")
    public String submit(@RequestParam("apiList") List<String> apiListJson) throws JsonProcessingException {

        // 각 API와 파라미터 목록을 처리
        List<Map<String, Object>> apiList = new ArrayList<>();
        for (String apiJson : apiListJson) {
            // API 정보와 파라미터를 JSON 파싱해서 처리
            Map<String, Object> apiData = new ObjectMapper().readValue(apiJson, Map.class);
            apiList.add(apiData);
            log.info("apiData: {}",apiJson);
        }


        return "redirect:/";
    }
}
