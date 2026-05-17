package com.novel.config;

import com.novel.entity.ModelConfig;
import com.novel.service.ModelConfigService;
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

    /**
     * Simple in-memory model registry.
     * Maps model name -> provider-specific config map.
     */
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
}