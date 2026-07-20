package com.novel.service;

import com.novel.entity.ChatMessage;

import java.util.List;

public interface ChatHistoryService {

    List<ChatMessage> getHistory(Long novelId);

    ChatMessage saveMessage(Long novelId, String role, String content, String refs, String changes);

    void clearHistory(Long novelId);
}
