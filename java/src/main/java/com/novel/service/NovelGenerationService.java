package com.novel.service;

public interface NovelGenerationService {

    String generateSynopsis(Long novelId, String modelName);

    String generateOutline(Long novelId, String modelName);

    String generateChapterSummary(Long novelId, Long chapterId, String modelName);

    String generateChapterContent(Long novelId, Long chapterId, String modelName);
}
