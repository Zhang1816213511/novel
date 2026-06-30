package com.novel.service;

import com.novel.dto.ChatRequest;
import com.novel.dto.ChatResponse;

public interface ChatService {

    ChatResponse chat(Long novelId, ChatRequest request);
}
