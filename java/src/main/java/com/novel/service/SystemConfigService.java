package com.novel.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.novel.entity.SystemConfig;
import com.novel.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private static final String KEY_WORKSPACE_ROOT = "app.workspace_root";

    private final SystemConfigMapper systemConfigMapper;

    /**
     * Caffeine 本地缓存，5 分钟 TTL。
     * 配置变更不频繁，5 分钟足够避免反复读库。
     */
    private final Cache<String, String> configCache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(50)
            .build();

    // ========== 通用配置 ==========

    /**
     * 获取配置值（走缓存）
     */
    public String getConfig(String key) {
        return configCache.get(key, k -> {
            SystemConfig config = systemConfigMapper.selectByKey(k);
            return config != null ? config.getConfigValue() : null;
        });
    }

    /**
     * 设置配置值（写入 DB 并失效缓存）
     */
    public void setConfig(String key, String value) {
        systemConfigMapper.upsertConfig(key, value);
        configCache.invalidate(key);
    }

    // ========== 工作目录 ==========

    /**
     * 获取全局工作目录路径（已标准化）
     */
    public String getWorkspaceRoot() {
        String raw = getConfig(KEY_WORKSPACE_ROOT);
        if (raw == null || raw.isBlank()) return null;
        return normalizePath(raw);
    }

    /**
     * 设置全局工作目录路径（自动标准化）
     */
    public void setWorkspaceRoot(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("工作目录路径不能为空");
        }
        String normalized = normalizePath(path);
        setConfig(KEY_WORKSPACE_ROOT, normalized);
        log.info("Workspace root set to: {}", normalized);
    }

    /**
     * 是否已配置工作目录
     */
    public boolean isConfigured() {
        return getWorkspaceRoot() != null;
    }

    // ========== 工具方法 ==========

    /**
     * 标准化路径：normalize + 绝对路径 + 统一斜杠
     */
    public static String normalizePath(String path) {
        return Paths.get(path).normalize().toAbsolutePath().toString()
                .replace('\\', '/');
    }
}
