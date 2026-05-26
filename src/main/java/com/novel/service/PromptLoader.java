package com.novel.service;

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
public class PromptLoader {

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
                // 去掉 .md 后缀作为 key
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

    /**
     * 获取原始提示词内容
     */
    public String get(String name) {
        String prompt = cache.get(name);
        if (prompt == null) {
            log.warn("Prompt not found: {}", name);
            return "";
        }
        return prompt;
    }

    /**
     * 获取提示词并替换占位符 {{key}}
     */
    public String get(String name, Map<String, String> variables) {
        String template = get(name);
        if (template.isEmpty()) return template;

        String result = template;
        // 先处理条件块 {{#key}}...{{/key}}
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
                    // 条件不满足，移除整个块
                    result = result.substring(0, startIdx) + result.substring(endIdx + conditionEnd.length());
                } else {
                    // 条件满足，只移除标记，保留内容
                    String before = result.substring(0, startIdx);
                    String after = result.substring(endIdx + conditionEnd.length());
                    result = before + blockContent + after;
                }
                startIdx = result.indexOf(conditionStart, startIdx);
            }
        }

        // 替换普通占位符 {{key}}
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * 重新加载所有提示词（用于热更新）
     */
    public void reload() {
        cache.clear();
        init();
    }
}
