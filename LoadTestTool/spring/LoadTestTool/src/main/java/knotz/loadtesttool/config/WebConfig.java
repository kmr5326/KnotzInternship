package knotz.loadtesttool.config;

import knotz.loadtesttool.loadTest.util.StringToMapConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    // string으로 입력된 json 값을 Map<String, String>으로 변경
    @Autowired
    private StringToMapConverter stringToMapConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToMapConverter);
    }

//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("http://61.37.80.126")
//                .allowedOrigins("http://127.0.0.1")
//                .allowedMethods("*")
//                .allowCredentials(true);
//    }
}
