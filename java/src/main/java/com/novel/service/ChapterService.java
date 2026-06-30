package com.novel.service;

import com.novel.entity.Chapter;

import java.util.List;

public interface ChapterService {

    List<Chapter> listByNovelId(Long novelId);

    Chapter getChapterByNumber(Long novelId, int chapterNumber);

    Chapter getById(Long id);

    Chapter createChapter(Long novelId);

    void updateChapter(Long id, Chapter chapter);

    void deleteChapter(Long id);
}
