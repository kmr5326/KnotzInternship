package knotz.loadtesttool.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class LoadTestController {

    @GetMapping("")
    public String index(Model model){
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
}
