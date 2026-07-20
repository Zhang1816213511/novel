package com.novel.service.impl;

import com.novel.entity.ChatMessage;
import com.novel.mapper.ChatMessageMapper;
import com.novel.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {

    private final ChatMessageMapper mapper;

    @Override
    public List<ChatMessage> getHistory(Long novelId) {
        return mapper.listByNovelId(novelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage saveMessage(Long novelId, String role, String content, String refs, String changes) {
        ChatMessage msg = new ChatMessage();
        msg.setNovelId(novelId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setRefs(refs);
        msg.setChanges(changes);
        mapper.insert(msg);
        msg.setId(mapper.getLastInsertId());
        return msg;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearHistory(Long novelId) {
        mapper.deleteByNovelId(novelId);
    }
}
