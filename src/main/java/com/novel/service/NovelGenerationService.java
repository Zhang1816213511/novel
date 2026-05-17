package com.novel.service;

import com.novel.entity.Chapter;
import com.novel.entity.Novel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelGenerationService {

    private static final int MAX_RETRIES = 3;

    private final LLMService llmService;
    private final NovelService novelService;
    private final ChapterService chapterService;

    private static final String GEN_SYSTEM = "你是一位专业的小说创作助手，擅长创作引人入胜的故事情节。你的文笔优美，叙事流畅。";
    private static final String REV_SYSTEM = "你是一位资深小说编辑，请检查内容质量。合格请回复「合格」，否则给出具体修改建议。";

    /**
     * 根据作品标题生成简介
     */
    public String generateSynopsis(Long novelId, String modelName) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在: " + novelId);

        String synopsis = generateWithReview(modelName,
            "请根据以下小说标题，创作一份作品简介（200-300字），包含故事背景和核心冲突。\n\n标题：" + novel.getTitle());

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

        String outline = generateWithReview(modelName,
            "请根据以下作品简介，创作完整的小说大纲（开端、发展、高潮、结局）。\n\n简介：" + novel.getSynopsis());

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

        // 获取之前章节的梗概作为上下文
        List<Chapter> allChapters = chapterService.listByNovelId(novelId);
        String previousSummaries = allChapters.stream()
            .filter(c -> c.getChapterNumber() < chapter.getChapterNumber())
            .map(c -> "第" + c.getChapterNumber() + "章梗概：" + (c.getSummary() != null ? c.getSummary() : "（无）"))
            .collect(Collectors.joining("\n\n"));

        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下大纲，创作第").append(chapter.getChapterNumber()).append("章的梗概。\n\n");
        prompt.append("大纲：\n").append(novel.getOutline()).append("\n\n");

        if (!previousSummaries.isBlank()) {
            prompt.append("前情提要（之前章节的梗概）：\n").append(previousSummaries).append("\n\n");
            prompt.append("请确保新章节的梗概与前一章节自然衔接，情节连贯。\n");
        }

        String summary = generateWithReview(modelName, prompt.toString());

        chapter.setSummary(summary);
        chapterService.updateById(chapter);
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

        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下章节梗概创作正文（2000-4000字），语言生动、描写细致、对话自然。\n\n");
        prompt.append("梗概：\n").append(chapter.getSummary()).append("\n\n");

        if (novel != null && novel.getOutline() != null && !novel.getOutline().isBlank()) {
            prompt.append("作品大纲参考：\n").append(novel.getOutline()).append("\n\n");
        }

        // 添加上一章正文结尾作为上下文
        String previousContent = allChapters.stream()
            .filter(c -> c.getChapterNumber() < chapter.getChapterNumber() && c.getContent() != null && !c.getContent().isBlank())
            .sorted((a, b) -> b.getChapterNumber() - a.getChapterNumber())
            .findFirst()
            .map(c -> c.getContent())
            .orElse(null);

        if (previousContent != null) {
            prompt.append("前一章正文结尾（用于衔接）：\n")
                .append(previousContent.substring(Math.max(0, previousContent.length() - 500)))
                .append("\n\n");
        }

        String content = generateWithReview(modelName, prompt.toString());

        chapter.setContent(content);
        chapterService.updateById(chapter);
        log.info("Content generated for chapter {} of novel {}", chapter.getChapterNumber(), novelId);
        return content;
    }

    // ========== Internal ==========

    private String generateWithReview(String modelName, String prompt) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            String result = llmService.chat(modelName, GEN_SYSTEM, prompt + "\n\n请直接输出内容，不要额外说明。");
            String review = llmService.chat(modelName, REV_SYSTEM,
                "请审查以下内容，合格请回复「合格」，否则指出问题：\n\n" + result);

            if (review != null && review.contains("合格")) {
                return result;
            }

            log.warn("Review failed attempt {}/{}: {}", attempt + 1, MAX_RETRIES, review);
            prompt += "\n\n修改意见：" + review + "\n请根据意见修改。";
        }
        log.warn("Max retries reached, using last result");
        return llmService.chat(modelName, GEN_SYSTEM, prompt + "\n\n请直接输出内容，不要额外说明。");
    }
}
