package com.novel.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    private final Environment env;

    public CacheConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public CacheManager cacheManager() {
        int expireAfterWrite = Integer.parseInt(env.getProperty("cache.expire", "3600"));

        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(expireAfterWrite, TimeUnit.SECONDS)
                .recordStats());

        return cacheManager;
    }
}