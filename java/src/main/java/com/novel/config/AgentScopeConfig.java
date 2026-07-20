package com.novel.config;

import com.novel.dto.ModelProperties;
import com.novel.entity.ModelConfig;
import com.novel.service.ModelConfigService;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.OpenAIChatModel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AgentScopeConfig {

    private final ModelConfigService modelConfigService;

    private final Map<String, ModelProperties> modelRegistry = new ConcurrentHashMap<>();

    @PostConstruct
    public void initModels() {
        var enabledModels = modelConfigService.listEnabled();
        for (ModelConfig mc : enabledModels) {
            try {
                ModelProperties props = new ModelProperties(
                    mc.getProvider(),
                    mc.getModelName(),
                    mc.getBaseUrl() != null ? mc.getBaseUrl() :
                        "ollama".equals(mc.getProvider()) ? "http://localhost:11434" :
                        "deepseek".equals(mc.getProvider()) ? "https://api.deepseek.com" : null,
                    mc.getApiKey(),
                    mc.getOptions()
                );
                modelRegistry.put(mc.getName(), props);
                log.info("已注册模型: {} ({})", mc.getName(), mc.getProvider());
            } catch (Exception e) {
                log.warn("注册模型失败: {}", mc.getName(), e);
            }
        }
        log.info("AgentScope 初始化完成，共 {} 个模型", enabledModels.size());
    }

    public ModelProperties getModel(String name) {
        return modelRegistry.get(name);
    }

    public Map<String, ModelProperties> getAllModels() {
        return modelRegistry;
    }

    public void registerModel(ModelConfig mc) {
        ModelProperties props = new ModelProperties(
            mc.getProvider(),
            mc.getModelName(),
            mc.getBaseUrl() != null ? mc.getBaseUrl() :
                "ollama".equals(mc.getProvider()) ? "http://localhost:11434" :
                "deepseek".equals(mc.getProvider()) ? "https://api.deepseek.com" : null,
            mc.getApiKey(),
            mc.getOptions()
        );
        modelRegistry.put(mc.getName(), props);
        log.info("已注册模型: {} ({})", mc.getName(), mc.getProvider());
    }

    public void unregisterModel(String name) {
        modelRegistry.remove(name);
        log.info("已注销模型: {}", name);
    }

    public Model buildModel(String name) {
        ModelProperties config = modelRegistry.get(name);
        if (config == null) {
            var mc = modelConfigService.listEnabled().stream()
                    .filter(m -> m.getName().equals(name))
                    .findFirst().orElse(null);
            if (mc != null) {
                registerModel(mc);
                config = modelRegistry.get(name);
            }
        }
        if (config == null) {
            throw new IllegalArgumentException("模型未找到: " + name);
        }

        if ("ollama".equals(config.getProvider())) {
            return OllamaChatModel.builder()
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .build();
        }

        if ("deepseek".equals(config.getProvider())) {
            return OpenAIChatModel.builder()
                    .baseUrl(config.getBaseUrl() != null ? config.getBaseUrl() : "https://api.deepseek.com")
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .build();
        }

        return OpenAIChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .build();
    }
}
