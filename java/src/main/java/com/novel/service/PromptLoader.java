package com.novel.service;

import java.util.Map;

public interface PromptLoader {

    String get(String name);

    String get(String name, Map<String, String> variables);

    void reload();
}
