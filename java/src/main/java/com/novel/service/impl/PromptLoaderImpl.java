package com.novel.service.impl;

import com.novel.service.PromptLoader;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptLoaderImpl implements PromptLoader {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private static final String PROMPTS_DIR = "classpath:prompts/*.md";

    @PostConstruct
    public void init() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(PROMPTS_DIR);
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;
                String key = filename.replace(".md", "");
                String content = resource.getContentAsString(StandardCharsets.UTF_8).trim();
                cache.put(key, content);
                log.debug("Loaded prompt: {}", key);
            }
            log.info("Loaded {} prompt files", cache.size());
        } catch (IOException e) {
            log.error("Failed to load prompt files", e);
        }
    }

    @Override
    public String get(String name) {
        String prompt = cache.get(name);
        if (prompt == null) {
            log.warn("Prompt not found: {}", name);
            return "";
        }
        return prompt;
    }

    @Override
    public String get(String name, Map<String, String> variables) {
        String template = get(name);
        if (template.isEmpty()) return template;

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String tag = entry.getKey();
            String value = entry.getValue();
            String conditionStart = "{{#" + tag + "}}";
            String conditionEnd = "{{/" + tag + "}}";

            int startIdx = result.indexOf(conditionStart);
            while (startIdx != -1) {
                int endIdx = result.indexOf(conditionEnd, startIdx);
                if (endIdx == -1) break;

                String blockContent = result.substring(startIdx + conditionStart.length(), endIdx);
                if (value == null || value.isBlank()) {
                    result = result.substring(0, startIdx) + result.substring(endIdx + conditionEnd.length());
                } else {
                    String before = result.substring(0, startIdx);
                    String after = result.substring(endIdx + conditionEnd.length());
                    result = before + blockContent + after;
                }
                startIdx = result.indexOf(conditionStart, startIdx);
            }
        }

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    @Override
    public void reload() {
        cache.clear();
        init();
    }
}
