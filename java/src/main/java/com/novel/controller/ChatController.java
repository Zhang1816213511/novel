package com.novel.controller;

import com.novel.common.Result;
import com.novel.dto.ChatRequest;
import com.novel.dto.ChatResponse;
import com.novel.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "AI 对话", description = "通过对话调用 agent 修改作品内容")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{novelId}")
    @Operation(summary = "发送对话消息")
    public Result<ChatResponse> chat(@PathVariable Long novelId, @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(novelId, request);
        return Result.success(response);
    }
}
