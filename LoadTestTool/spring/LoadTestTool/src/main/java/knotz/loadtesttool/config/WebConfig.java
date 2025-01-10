package knotz.loadtesttool.config;

import knotz.loadtesttool.loadTest.util.StringToMapConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // string으로 입력된 json 값을 Map<String, String>으로 변경
    @Autowired
    private StringToMapConverter stringToMapConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToMapConverter);
    }
}
