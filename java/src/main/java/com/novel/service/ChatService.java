package com.novel.service;

import com.novel.dto.ChatRequest;
import com.novel.dto.ChatResponse;
import com.novel.dto.ChatResponse.FileChange;
import com.novel.entity.Chapter;
import com.novel.entity.Novel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 对话服务 — 用户通过聊天对话调用 agent 修改作品内容。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final NovelService novelService;
    private final ChapterService chapterService;
    private final MarkdownStorageService markdownStorage;
    private final AgentPipelineService agentPipeline;
    private final PromptLoader promptLoader;

    /**
     * 处理用户聊天消息
     */
    public ChatResponse chat(Long novelId, ChatRequest request) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在");

        // 1. 收集引用文件的上下文
        String context = buildContext(novel, request);

        // 2. 构建 prompt 并调用 agent
        String userPrompt = buildUserPrompt(request.getMessage(), context);
        String agentOutput = agentPipeline.generateWithReview(
                request.getModelName(),
                promptLoader.get("chat-editor-system"),
                promptLoader.get("reviewer-system"),
                userPrompt
        );

        // 3. 解析 agent 输出，提取改动
        return parseResponse(agentOutput, novel, request);
    }

    /**
     * 收集引用文件的完整内容作为上下文
     */
    private String buildContext(Novel novel, ChatRequest request) {
        if (request.getRefs() == null || request.getRefs().isEmpty()) {
            // 没有显式引用时，提供当前作品的概要上下文
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

    /**
     * 没有显式引用时提供概要信息
     */
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

    /**
     * 截取文件指定行范围
     */
    private String applyLineRange(String content, ChatRequest.FileRef ref) {
        if (content == null || content.isBlank()) return content;
        if (ref.getStartLine() == null) return content;

        String[] lines = content.split("\n", -1);
        int start = Math.max(0, ref.getStartLine() - 1);
        int end = (ref.getEndLine() != null) ? Math.min(ref.getEndLine(), lines.length) : lines.length;
        if (start >= lines.length) return "";

        return String.join("\n", java.util.Arrays.copyOfRange(lines, start, end));
    }

    /**
     * 构建发给 agent 的用户 prompt
     */
    private String buildUserPrompt(String message, String context) {
        return "## 上下文\n\n" + context + "\n\n## 用户要求\n\n" + message;
    }

    /**
     * 解析 agent 输出，提取改动并保存到文件
     */
    private ChatResponse parseResponse(String agentOutput, Novel novel, ChatRequest request) {
        if (agentOutput == null || agentOutput.isBlank()) {
            return ChatResponse.builder().reply("（没有生成回复）").build();
        }

        String reply;
        List<FileChange> changes = new ArrayList<>();

        if (agentOutput.contains("[改动]")) {
            // 有改动 — 解析改动部分
            String[] parts = agentOutput.split("\\[改动\\]", 2);
            String changeSection = parts.length > 1 ? parts[1] : "";
            reply = "已完成修改：" + extractSummary(changeSection);

            // 提取实际改动内容（改动标记之后的内容块）
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
        // 提取改动说明
        int idx = changeSection.indexOf("说明：");
        if (idx >= 0) {
            int end = changeSection.indexOf("\n", idx);
            if (end > idx) return changeSection.substring(idx + 3, end).trim();
        }
        return "请查看更新后的内容";
    }

    /**
     * 从 agent 输出中提取修改后的内容
     * 格式：[改动] 标记后的完整内容即为修改结果
     */
    private String extractModifiedContent(String changeSection) {
        // 跳过文件标记行，提取实际内容
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

    /**
     * 应用改动 — 将 agent 返回的新内容写回文件，并记录 change
     */
    private void applyChanges(String modifiedContent, Novel novel, ChatRequest request, List<FileChange> changes) {
        if (request.getRefs() == null || request.getRefs().isEmpty() || novel.getWorkspaceDir() == null) return;

        for (ChatRequest.FileRef ref : request.getRefs()) {
            switch (ref.getType()) {
                case "synopsis":
                    markdownStorage.writeSynopsis(novel.getWorkspaceDir(), modifiedContent);
                    changes.add(FileChange.builder().type("synopsis").content(modifiedContent).build());
                    break;
                case "outline":
                    markdownStorage.writeOutline(novel.getWorkspaceDir(), modifiedContent);
                    changes.add(FileChange.builder().type("outline").content(modifiedContent).build());
                    break;
                case "chapter":
                    if (ref.getChapterNumber() != null) {
                        Chapter ch = chapterService.getChapterByNumber(novel.getId(), ref.getChapterNumber());
                        if (ch != null && ch.getMdDir() != null) {
                            if (ref.getSection() == null || "summary".equals(ref.getSection())) {
                                markdownStorage.writeChapterSummary(novel.getWorkspaceDir(), ch.getMdDir(), modifiedContent);
                                changes.add(FileChange.builder()
                                        .type("chapter").chapterNumber(ref.getChapterNumber())
                                        .section("summary").content(modifiedContent).build());
                            }
                            if (ref.getSection() == null || "content".equals(ref.getSection())) {
                                markdownStorage.writeChapterContent(novel.getWorkspaceDir(), ch.getMdDir(), modifiedContent);
                                changes.add(FileChange.builder()
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
