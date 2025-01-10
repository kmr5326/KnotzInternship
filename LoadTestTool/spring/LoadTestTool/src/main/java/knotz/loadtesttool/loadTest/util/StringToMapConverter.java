package knotz.loadtesttool.loadTest.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class StringToMapConverter implements Converter<String, Map<String, String>> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, String> convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null; // 또는 빈 맵을 반환하려면 new HashMap<>()
        }
        try {
            return objectMapper.readValue(source, new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
