package com.novel.controller;

import com.novel.common.Result;
import com.novel.dto.ChatRequest;
import com.novel.dto.ChatResponse;
import com.novel.entity.ChatMessage;
import com.novel.service.ChatHistoryService;
import com.novel.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "通过对话调用 agent 修改作品内容")
public class ChatController {

    private final ChatService chatService;
    private final ChatHistoryService chatHistoryService;

    @PostMapping("/{novelId}")
    @Operation(summary = "发送对话消息")
    public Result<ChatResponse> chat(@PathVariable Long novelId, @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(novelId, request);
        return Result.success(response);
    }

    @GetMapping("/{novelId}/history")
    @Operation(summary = "获取对话历史")
    public Result<List<ChatMessage>> getHistory(@PathVariable Long novelId) {
        return Result.success(chatHistoryService.getHistory(novelId));
    }

    @DeleteMapping("/{novelId}/history")
    @Operation(summary = "清空对话历史")
    public Result<Void> clearHistory(@PathVariable Long novelId) {
        chatHistoryService.clearHistory(novelId);
        return Result.success();
    }
}
