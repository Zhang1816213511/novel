package com.novel.service;

import com.novel.config.AgentScopeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    @Autowired
    private AgentScopeConfig agentScopeConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用 LLM chat completion（OpenAI 兼容接口）
     * @param modelName  模型配置名称
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @param temperature 温度参数
     * @return 生成的文本
     */
    @SuppressWarnings("unchecked")
    public String chat(String modelName, String systemPrompt, String userMessage, double temperature) {
        Map<String, String> config = agentScopeConfig.getModel(modelName);
        if (config == null) {
            throw new IllegalArgumentException("Model not found: " + modelName);
        }

        String baseUrl = config.get("baseUrl");
        String apiKey = config.get("apiKey");
        String actualModel = config.get("modelName");

        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/v1/chat/completions";

        Map<String, Object> requestBody = Map.of(
            "model", actualModel,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage)
            ),
            "temperature", temperature
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        try {
            var response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Empty response from LLM");

            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) throw new RuntimeException("No choices in LLM response");

            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) throw new RuntimeException("No message in LLM response choice");

            return (String) message.get("content");
        } catch (Exception e) {
            log.error("LLM call failed: model={}, error={}", modelName, e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    public String chat(String modelName, String systemPrompt, String userMessage) {
        return chat(modelName, systemPrompt, userMessage, 0.7);
    }
}
