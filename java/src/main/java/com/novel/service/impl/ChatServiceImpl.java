package com.novel.service.impl;

import com.novel.dto.ChatRequest;
import com.novel.dto.ChatResponse;
import com.novel.entity.Chapter;
import com.novel.entity.Novel;
import com.novel.service.AgentPipelineService;
import com.novel.service.ChapterService;
import com.novel.service.ChatService;
import com.novel.service.MarkdownStorageService;
import com.novel.service.NovelService;
import com.novel.service.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final NovelService novelService;
    private final ChapterService chapterService;
    private final MarkdownStorageService markdownStorage;
    private final AgentPipelineService agentPipeline;
    private final PromptLoader promptLoader;

    @Override
    public ChatResponse chat(Long novelId, ChatRequest request) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在");

        String context = buildContext(novel, request);
        String userPrompt = buildUserPrompt(request.getMessage(), context);
        String agentOutput = agentPipeline.generateWithReview(
                request.getModelName(),
                promptLoader.get("chat-editor-system"),
                promptLoader.get("reviewer-system"),
                userPrompt
        );

        return parseResponse(agentOutput, novel, request);
    }

    private String buildContext(Novel novel, ChatRequest request) {
        if (request.getRefs() == null || request.getRefs().isEmpty()) {
            return buildDefaultContext(novel);
        }

        StringBuilder ctx = new StringBuilder();
        for (ChatRequest.FileRef ref : request.getRefs()) {
            switch (ref.getType()) {
                case "synopsis":
                    String syn = markdownStorage.readSynopsis(novel.getWorkspaceDir());
                    ctx.append("【作品简介】\n").append(syn != null ? syn : "（无）").append("\n\n");
                    break;
                case "outline":
                    String out = markdownStorage.readOutline(novel.getWorkspaceDir());
                    ctx.append("【创作大纲】\n").append(out != null ? out : "（无）").append("\n\n");
                    break;
                case "chapter":
                    Chapter ch = chapterService.getChapterByNumber(novel.getId(), ref.getChapterNumber());
                    if (ch != null && novel.getWorkspaceDir() != null && ch.getMdDir() != null) {
                        if (ref.getSection() == null || "summary".equals(ref.getSection())) {
                            String summary = markdownStorage.readChapterSummary(novel.getWorkspaceDir(), ch.getMdDir());
                            ctx.append("【第").append(ref.getChapterNumber()).append("章 梗概】\n");
                            ctx.append(applyLineRange(summary, ref)).append("\n\n");
                        }
                        if (ref.getSection() == null || "content".equals(ref.getSection())) {
                            String content = markdownStorage.readChapterContent(novel.getWorkspaceDir(), ch.getMdDir());
                            ctx.append("【第").append(ref.getChapterNumber()).append("章 正文】\n");
                            ctx.append(applyLineRange(content, ref)).append("\n\n");
                        }
                    }
                    break;
            }
        }
        return ctx.toString();
    }

    private String buildDefaultContext(Novel novel) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("作品标题：").append(novel.getTitle()).append("\n\n");
        if (novel.getSynopsis() != null && !novel.getSynopsis().isBlank()) {
            ctx.append("【作品简介】\n").append(novel.getSynopsis()).append("\n\n");
        }
        if (novel.getOutline() != null && !novel.getOutline().isBlank()) {
            ctx.append("【创作大纲】\n").append(novel.getOutline()).append("\n\n");
        }
        List<Chapter> chapters = chapterService.listByNovelId(novel.getId());
        if (!chapters.isEmpty()) {
            ctx.append("共 ").append(chapters.size()).append(" 章\n");
            for (Chapter ch : chapters) {
                ctx.append("第").append(ch.getChapterNumber()).append("章：").append(ch.getTitle()).append("\n");
            }
        }
        return ctx.toString();
    }

    private String applyLineRange(String content, ChatRequest.FileRef ref) {
        if (content == null || content.isBlank()) return content;
        if (ref.getStartLine() == null) return content;

        String[] lines = content.split("\n", -1);
        int start = Math.max(0, ref.getStartLine() - 1);
        int end = (ref.getEndLine() != null) ? Math.min(ref.getEndLine(), lines.length) : lines.length;
        if (start >= lines.length) return "";

        return String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
    }

    private String buildUserPrompt(String message, String context) {
        return "## 上下文\n\n" + context + "\n\n## 用户要求\n\n" + message;
    }

    private ChatResponse parseResponse(String agentOutput, Novel novel, ChatRequest request) {
        if (agentOutput == null || agentOutput.isBlank()) {
            return ChatResponse.builder().reply("（没有生成回复）").build();
        }

        String reply;
        List<ChatResponse.FileChange> changes = new ArrayList<>();

        if (agentOutput.contains("[改动]")) {
            String[] parts = agentOutput.split("\\[改动\\]", 2);
            String changeSection = parts.length > 1 ? parts[1] : "";
            reply = "已完成修改：" + extractSummary(changeSection);

            String modifiedContent = extractModifiedContent(changeSection);
            if (!modifiedContent.isBlank()) {
                applyChanges(modifiedContent, novel, request, changes);
            }
        } else {
            reply = agentOutput;
        }

        return ChatResponse.builder()
                .reply(reply)
                .changes(changes.isEmpty() ? null : changes)
                .build();
    }

    private String extractSummary(String changeSection) {
        int idx = changeSection.indexOf("说明：");
        if (idx >= 0) {
            int end = changeSection.indexOf("\n", idx);
            if (end > idx) return changeSection.substring(idx + 3, end).trim();
        }
        return "请查看更新后的内容";
    }

    private String extractModifiedContent(String changeSection) {
        String[] lines = changeSection.split("\n", -1);
        StringBuilder content = new StringBuilder();
        boolean inContent = false;
        for (String line : lines) {
            if (line.startsWith("- 文件:") || line.startsWith("  说明:")) {
                inContent = false;
                continue;
            }
            if (line.trim().isEmpty() && !inContent) {
                continue;
            }
            if (!inContent && !line.startsWith("-") && !line.startsWith("  说明:")) {
                inContent = true;
            }
            if (inContent) {
                if (content.length() > 0) content.append("\n");
                content.append(line);
            }
        }
        return content.toString().trim();
    }

    private void applyChanges(String modifiedContent, Novel novel, ChatRequest request, List<ChatResponse.FileChange> changes) {
        if (request.getRefs() == null || request.getRefs().isEmpty() || novel.getWorkspaceDir() == null) return;

        for (ChatRequest.FileRef ref : request.getRefs()) {
            switch (ref.getType()) {
                case "synopsis":
                    markdownStorage.writeSynopsis(novel.getWorkspaceDir(), modifiedContent);
                    changes.add(ChatResponse.FileChange.builder().type("synopsis").content(modifiedContent).build());
                    break;
                case "outline":
                    markdownStorage.writeOutline(novel.getWorkspaceDir(), modifiedContent);
                    changes.add(ChatResponse.FileChange.builder().type("outline").content(modifiedContent).build());
                    break;
                case "chapter":
                    if (ref.getChapterNumber() != null) {
                        Chapter ch = chapterService.getChapterByNumber(novel.getId(), ref.getChapterNumber());
                        if (ch != null && ch.getMdDir() != null) {
                            if (ref.getSection() == null || "summary".equals(ref.getSection())) {
                                markdownStorage.writeChapterSummary(novel.getWorkspaceDir(), ch.getMdDir(), modifiedContent);
                                changes.add(ChatResponse.FileChange.builder()
                                        .type("chapter").chapterNumber(ref.getChapterNumber())
                                        .section("summary").content(modifiedContent).build());
                            }
                            if (ref.getSection() == null || "content".equals(ref.getSection())) {
                                markdownStorage.writeChapterContent(novel.getWorkspaceDir(), ch.getMdDir(), modifiedContent);
                                changes.add(ChatResponse.FileChange.builder()
                                        .type("chapter").chapterNumber(ref.getChapterNumber())
                                        .section("content").content(modifiedContent).build());
                            }
                        }
                    }
                    break;
            }
        }
    }
}
