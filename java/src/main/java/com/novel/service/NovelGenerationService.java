package com.novel.service;

import com.novel.entity.Chapter;
import com.novel.entity.Novel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelGenerationService {

    private final AgentPipelineService agentPipelineService;
    private final NovelService novelService;
    private final ChapterService chapterService;
    private final MarkdownStorageService markdownStorage;
    private final PromptLoader promptLoader;

    /**
     * 根据作品标题生成简介
     */
    public String generateSynopsis(Long novelId, String modelName) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在: " + novelId);

        String userPrompt = promptLoader.get("generate-synopsis",
                Map.of("title", novel.getTitle() != null ? novel.getTitle() : ""));
        String synopsis = agentPipelineService.generateWithReview(modelName,
                promptLoader.get("writer-system"),
                promptLoader.get("reviewer-system"),
                userPrompt);

        novel.setSynopsis(synopsis);
        novelService.updateNovel(novel.getId(), novel);
        log.info("Synopsis generated for novel {}", novelId);
        return synopsis;
    }

    /**
     * 根据作品简介生成大纲
     */
    public String generateOutline(Long novelId, String modelName) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在: " + novelId);
        if (novel.getSynopsis() == null || novel.getSynopsis().isBlank())
            throw new IllegalStateException("请先生成作品简介");

        String userPrompt = promptLoader.get("generate-outline",
                Map.of("synopsis", novel.getSynopsis()));
        String outline = agentPipelineService.generateWithReview(modelName,
                promptLoader.get("writer-system"),
                promptLoader.get("reviewer-system"),
                userPrompt);

        novel.setOutline(outline);
        novelService.updateNovel(novel.getId(), novel);
        log.info("Outline generated for novel {}", novelId);
        return outline;
    }

    /**
     * 根据大纲和前几章的梗概，生成指定章节的梗概
     */
    public String generateChapterSummary(Long novelId, Long chapterId, String modelName) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在: " + novelId);
        if (novel.getOutline() == null || novel.getOutline().isBlank())
            throw new IllegalStateException("请先生成作品大纲");

        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null) throw new IllegalArgumentException("章节不存在: " + chapterId);

        List<Chapter> allChapters = chapterService.listByNovelId(novelId);
        String previousSummaries = allChapters.stream()
            .filter(c -> c.getChapterNumber() < chapter.getChapterNumber())
            .map(c -> "第" + c.getChapterNumber() + "章梗概：" + (c.getSummary() != null ? c.getSummary() : "（无）"))
            .collect(Collectors.joining("\n\n"));

        Map<String, String> vars = new HashMap<>();
        vars.put("chapterNumber", String.valueOf(chapter.getChapterNumber()));
        vars.put("outline", novel.getOutline());
        vars.put("previousSummaries", previousSummaries);
        String userPrompt = promptLoader.get("generate-chapter-summary", vars);

        String summary = agentPipelineService.generateWithReview(modelName,
                promptLoader.get("writer-system"),
                promptLoader.get("reviewer-system"),
                userPrompt);

        // 写入 md 文件
        if (novel.getWorkspaceDir() != null && chapter.getMdDir() != null) {
            markdownStorage.writeChapterSummary(novel.getWorkspaceDir(), chapter.getMdDir(), summary);
        }
        log.info("Summary generated for chapter {} of novel {}", chapter.getChapterNumber(), novelId);
        return summary;
    }

    /**
     * 根据章节梗概生成正文
     */
    public String generateChapterContent(Long novelId, Long chapterId, String modelName) {
        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null) throw new IllegalArgumentException("章节不存在: " + chapterId);
        if (chapter.getSummary() == null || chapter.getSummary().isBlank())
            throw new IllegalStateException("请先生成或填写本章梗概");

        Novel novel = novelService.getById(novelId);
        List<Chapter> allChapters = chapterService.listByNovelId(novelId);

        // 添加上一章正文结尾作为上下文
        String previousContent = allChapters.stream()
            .filter(c -> c.getChapterNumber() < chapter.getChapterNumber() && c.getContent() != null && !c.getContent().isBlank())
            .sorted((a, b) -> b.getChapterNumber() - a.getChapterNumber())
            .findFirst()
            .map(c -> c.getContent())
            .orElse(null);

        String prevContentTail = previousContent != null
                ? previousContent.substring(Math.max(0, previousContent.length() - 500))
                : "";

        Map<String, String> vars = new HashMap<>();
        vars.put("summary", chapter.getSummary());
        vars.put("outline", novel != null && novel.getOutline() != null ? novel.getOutline() : "");
        vars.put("previousContent", prevContentTail);
        String userPrompt = promptLoader.get("generate-chapter-content", vars);

        String content = agentPipelineService.generateWithReview(modelName,
                promptLoader.get("writer-system"),
                promptLoader.get("reviewer-system"),
                userPrompt);

        // 写入 md 文件
        if (novel.getWorkspaceDir() != null && chapter.getMdDir() != null) {
            markdownStorage.writeChapterContent(novel.getWorkspaceDir(), chapter.getMdDir(), content);
        }
        log.info("Content generated for chapter {} of novel {}", chapter.getChapterNumber(), novelId);
        return content;
    }
}
