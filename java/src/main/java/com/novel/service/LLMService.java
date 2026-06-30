package com.novel.service;

import com.novel.config.AgentScopeConfig;
import com.novel.dto.ChatCompletionRequest;
import com.novel.dto.ChatCompletionResponse;
import com.novel.dto.ModelProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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
    public String chat(String modelName, String systemPrompt, String userMessage, double temperature) {
        ModelProperties config = agentScopeConfig.getModel(modelName);
        if (config == null) {
            throw new IllegalArgumentException("Model not found: " + modelName);
        }

        String baseUrl = config.getBaseUrl();
        if (baseUrl != null && baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String url = baseUrl + "/v1/chat/completions";

        ChatCompletionRequest requestBody = ChatCompletionRequest.builder()
            .model(config.getModelName())
            .messages(List.of(
                ChatCompletionRequest.Message.builder().role("system").content(systemPrompt).build(),
                ChatCompletionRequest.Message.builder().role("user").content(userMessage).build()
            ))
            .temperature(temperature)
            .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            headers.setBearerAuth(config.getApiKey());
        }

        try {
            var response = restTemplate.postForEntity(url, new HttpEntity<>(requestBody, headers), ChatCompletionResponse.class);
            ChatCompletionResponse body = response.getBody();
            if (body == null) throw new RuntimeException("Empty response from LLM");

            if (body.getChoices() == null || body.getChoices().isEmpty())
                throw new RuntimeException("No choices in LLM response");

            ChatCompletionResponse.Message message = body.getChoices().get(0).getMessage();
            if (message == null) throw new RuntimeException("No message in LLM response choice");

            return message.getContent();
        } catch (Exception e) {
            log.error("LLM call failed: model={}, error={}", modelName, e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    public String chat(String modelName, String systemPrompt, String userMessage) {
        return chat(modelName, systemPrompt, userMessage, 0.7);
    }
}
