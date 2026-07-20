package com.novel.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.novel.entity.SystemConfig;
import com.novel.mapper.SystemConfigMapper;
import com.novel.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String KEY_WORKSPACE_ROOT = "app.workspace_root";

    private final SystemConfigMapper systemConfigMapper;

    private final Cache<String, String> configCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();

    @Override
    public String getConfig(String key) {
        return configCache.get(key, k -> {
            SystemConfig config = systemConfigMapper.selectByKey(k);
            return config != null ? config.getConfigValue() : null;
        });
    }

    @Override
    public void setConfig(String key, String value) {
        systemConfigMapper.upsertConfig(key, value);
        configCache.invalidate(key);
    }

    @Override
    public String getWorkspaceRoot() {
        String raw = getConfig(KEY_WORKSPACE_ROOT);
        if (raw == null || raw.isBlank()) return null;
        return normalizePath(raw);
    }

    @Override
    public void setWorkspaceRoot(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("工作目录路径不能为空");
        }
        String normalized = normalizePath(path);
        setConfig(KEY_WORKSPACE_ROOT, normalized);
        log.info("工作目录根路径已设置: {}", normalized);
    }

    @Override
    public boolean isConfigured() {
        return getWorkspaceRoot() != null;
    }

    public static String normalizePath(String path) {
        return Paths.get(path).normalize().toAbsolutePath().toString()
                .replace('\\', '/');
    }
}
