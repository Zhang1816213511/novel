package com.novel.config;

import com.novel.entity.ModelConfig;
import com.novel.service.ModelConfigService;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentScopeConfig {

    @Autowired
    private ModelConfigService modelConfigService;

    private final Map<String, Map<String, String>> modelRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void initModels() {
        var enabledModels = modelConfigService.listEnabled();
        for (ModelConfig mc : enabledModels) {
            try {
                Map<String, String> props = new HashMap<>();
                props.put("provider", mc.getProvider());
                props.put("modelName", mc.getModelName());
                props.put("baseUrl", mc.getBaseUrl() != null ? mc.getBaseUrl() :
                    "ollama".equals(mc.getProvider()) ? "http://localhost:11434" : null);
                props.put("apiKey", mc.getApiKey());
                if (mc.getOptions() != null) {
                    props.put("options", mc.getOptions());
                }
                modelRegistry.put(mc.getName(), props);
                log.info("Registered model: {} ({})", mc.getName(), mc.getProvider());
            } catch (Exception e) {
                log.warn("Failed to register model: {}", mc.getName(), e);
            }
        }
        log.info("AgentScope initialized with {} models", enabledModels.size());
    }

    public Map<String, String> getModel(String name) {
        return modelRegistry.get(name);
    }

    public Map<String, Map<String, String>> getAllModels() {
        return modelRegistry;
    }

    /**
     * 根据模型名称构建 AgentScope Model 对象
     */
    public Model buildModel(String name) {
        Map<String, String> config = modelRegistry.get(name);
        if (config == null) {
            throw new IllegalArgumentException("Model not found: " + name);
        }

        String provider = config.get("provider");
        String baseUrl = config.get("baseUrl");
        String apiKey = config.get("apiKey");
        String modelName = config.get("modelName");

        if ("ollama".equals(provider)) {
            return OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .build();
        }

        return OpenAIChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}