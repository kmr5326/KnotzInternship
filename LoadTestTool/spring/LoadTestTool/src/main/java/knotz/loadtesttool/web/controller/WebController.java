package knotz.loadtesttool.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import knotz.loadtesttool.influxDB.service.InfluxDBService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
@Slf4j
public class WebController {

    private final InfluxDBService influxDBService;
    private final ResourceLoader resourceLoader;

    @GetMapping("")
    public String index(Model model) {
//        List<String> apis = Arrays.asList("AuthCookieAPI", "AuthLogoutAPI", "UserGetAllAPI");

        ObjectMapper objectMapper = new ObjectMapper();

//        Map<String, List<String>> apiParameters = new HashMap<>();
//        apiParameters.put("AuthCookieAPI", Arrays.asList("username", "password"));
//        apiParameters.put("UserGetAllAPI", Arrays.asList("name", "pagesize", "page"));
//        apiParameters.put("AuthLogoutAPI", Arrays.asList("username"));
        List<String> apis=new ArrayList<>();
        Map<String, List<String>> apiParameters = null;

        try {
            Resource resource = resourceLoader.getResource("classpath:api_config.json");
            apiParameters = objectMapper.readValue(resource.getInputStream(), Map.class);
            apis=List.copyOf(apiParameters.keySet());
        } catch (IOException e) {
            log.error("api-config.json File read Error : {}",e.getMessage());
            // 파일 읽기 오류 처리
        }

        try {
            String apiParametersJson = objectMapper.writeValueAsString(apiParameters);
            model.addAttribute("apiParameters", apiParametersJson);
        } catch (JsonProcessingException e) {
            log.error("apiParameters Json parsing Error: {}",e.getMessage());
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
    public String submit(@RequestParam("apiList") List<String> apiListJson,
                         @RequestParam("threads") Integer threads,
                         @RequestParam("rampUp") Integer rampUp,
                         @RequestParam("loopCount") Integer loopCount,
                         @RequestParam("duration") Integer duration) throws JsonProcessingException {

        log.info("threads: {}, rampUp: {}, loopCount: {}, duration: {}",threads,rampUp,loopCount,duration);

        List<Map<String, String>> apiList = new ArrayList<>();
        for (String apiJson : apiListJson) {
//            log.info("apiData: {}",apiJson);
            // API 정보와 파라미터를 JSON 파싱해서 처리
            Map<String, Object> apiData = new ObjectMapper().readValue(apiJson, Map.class);
            String apiName=apiData.get("api").toString();
            if(apiName.equals("null"))continue;
            Map<String,String> dataMap= new HashMap<>();
            dataMap.put("api",apiName);

            Map<String, Object> params = (Map<String, Object>) apiData.get("params");
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    dataMap.put(entry.getKey(), entry.getValue().toString());
                }
            }

            apiList.add(dataMap);

        }

//        for(Map<String,String> e:apiList){
//            e.forEach((key,value)->{
//                log.info("key: {} , value {}",key,value);
//            });
//        }

        return "redirect:/";
    }
}
