package com.novel.service.impl;

import com.novel.entity.Chapter;
import com.novel.entity.Novel;
import com.novel.service.AgentPipelineService;
import com.novel.service.ChapterService;
import com.novel.service.MarkdownStorageService;
import com.novel.service.NovelGenerationService;
import com.novel.service.NovelService;
import com.novel.service.PromptLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelGenerationServiceImpl implements NovelGenerationService {

    private final AgentPipelineService agentPipelineService;
    private final NovelService novelService;
    private final ChapterService chapterService;
    private final MarkdownStorageService markdownStorage;
    private final PromptLoader promptLoader;

    @Override
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
        log.info("作品简介已生成: novelId={}", novelId);
        return synopsis;
    }

    @Override
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
        log.info("作品大纲已生成: novelId={}", novelId);
        return outline;
    }

    @Override
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

        String userPrompt = promptLoader.get("generate-chapter-summary",
                Map.of("chapterNumber", String.valueOf(chapter.getChapterNumber()),
                       "outline", novel.getOutline(),
                       "previousSummaries", previousSummaries));

        String summary = agentPipelineService.generateWithReview(modelName,
                promptLoader.get("writer-system"),
                promptLoader.get("reviewer-system"),
                userPrompt);

        if (novel.getWorkspaceDir() != null && chapter.getMdDir() != null) {
            markdownStorage.writeChapterSummary(novel.getWorkspaceDir(), chapter.getMdDir(), summary);
        }
        log.info("章节梗概已生成: chapter={}, novelId={}", chapter.getChapterNumber(), novelId);
        return summary;
    }

    @Override
    public String generateChapterContent(Long novelId, Long chapterId, String modelName) {
        Chapter chapter = chapterService.getById(chapterId);
        if (chapter == null) throw new IllegalArgumentException("章节不存在: " + chapterId);
        if (chapter.getSummary() == null || chapter.getSummary().isBlank())
            throw new IllegalStateException("请先生成或填写本章梗概");

        Novel novel = novelService.getById(novelId);
        List<Chapter> allChapters = chapterService.listByNovelId(novelId);

        String previousContent = allChapters.stream()
            .filter(c -> c.getChapterNumber() < chapter.getChapterNumber() && c.getContent() != null && !c.getContent().isBlank())
            .sorted((a, b) -> b.getChapterNumber() - a.getChapterNumber())
            .findFirst()
            .map(c -> c.getContent())
            .orElse(null);

        String prevContentTail = previousContent != null
                ? previousContent.substring(Math.max(0, previousContent.length() - 500))
                : "";

        String userPrompt = promptLoader.get("generate-chapter-content",
                Map.of("summary", chapter.getSummary(),
                       "outline", novel != null && novel.getOutline() != null ? novel.getOutline() : "",
                       "previousContent", prevContentTail));

        String content = agentPipelineService.generateWithReview(modelName,
                promptLoader.get("writer-system"),
                promptLoader.get("reviewer-system"),
                userPrompt);

        if (novel.getWorkspaceDir() != null && chapter.getMdDir() != null) {
            markdownStorage.writeChapterContent(novel.getWorkspaceDir(), chapter.getMdDir(), content);
        }
        log.info("章节正文已生成: chapter={}, novelId={}", chapter.getChapterNumber(), novelId);
        return content;
    }
}
