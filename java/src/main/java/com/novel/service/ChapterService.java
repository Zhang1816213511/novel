package com.novel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novel.entity.Chapter;
import com.novel.mapper.ChapterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterService extends ServiceImpl<ChapterMapper, Chapter> {

    private final ChapterMapper chapterMapper;

    public List<Chapter> listByNovelId(Long novelId) {
        return chapterMapper.listByNovelId(novelId);
    }

    public Chapter createChapter(Long novelId) {
        int nextNumber = chapterMapper.getMaxChapterNumber(novelId) + 1;
        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setChapterNumber(nextNumber);
        chapter.setTitle("第" + nextNumber + "章");
        chapter.setSummary("");
        chapter.setContent("");
        save(chapter);
        return chapter;
    }
}
