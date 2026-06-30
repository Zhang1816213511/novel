package com.novel.service;

public interface SystemConfigService {

    String getConfig(String key);

    void setConfig(String key, String value);

    String getWorkspaceRoot();

    void setWorkspaceRoot(String path);

    boolean isConfigured();
}
