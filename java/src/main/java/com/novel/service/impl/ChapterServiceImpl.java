package com.novel.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.novel.entity.Chapter;
import com.novel.entity.Novel;
import com.novel.mapper.ChapterMapper;
import com.novel.service.ChapterService;
import com.novel.service.MarkdownStorageService;
import com.novel.service.NovelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl extends ServiceImpl<ChapterMapper, Chapter> implements ChapterService {

    private final ChapterMapper chapterMapper;
    private final NovelService novelService;
    private final MarkdownStorageService markdownStorage;

    @Override
    public List<Chapter> listByNovelId(Long novelId) {
        List<Chapter> chapters = chapterMapper.listByNovelId(novelId);
        Novel novel = novelService.getById(novelId);
        if (novel != null && novel.getWorkspaceDir() != null) {
            String workspace = novel.getWorkspaceDir();
            for (Chapter ch : chapters) {
                if (ch.getMdDir() != null) {
                    ch.setSummary(markdownStorage.readChapterSummary(workspace, ch.getMdDir()));
                    ch.setContent(markdownStorage.readChapterContent(workspace, ch.getMdDir()));
                }
            }
        }
        return chapters;
    }

    @Override
    public Chapter getChapterByNumber(Long novelId, int chapterNumber) {
        return chapterMapper.getByNovelIdAndNumber(novelId, chapterNumber);
    }

    @Override
    public Chapter getById(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter != null && chapter.getMdDir() != null) {
            Novel novel = novelService.getById(chapter.getNovelId());
            if (novel != null && novel.getWorkspaceDir() != null) {
                chapter.setSummary(markdownStorage.readChapterSummary(novel.getWorkspaceDir(), chapter.getMdDir()));
                chapter.setContent(markdownStorage.readChapterContent(novel.getWorkspaceDir(), chapter.getMdDir()));
            }
        }
        return chapter;
    }

    @Override
    public Chapter createChapter(Long novelId) {
        Novel novel = novelService.getById(novelId);
        if (novel == null) throw new IllegalArgumentException("作品不存在: " + novelId);

        int nextNumber = chapterMapper.getMaxChapterNumber(novelId) + 1;
        Chapter chapter = new Chapter();
        chapter.setNovelId(novelId);
        chapter.setChapterNumber(nextNumber);
        chapter.setTitle("第" + nextNumber + "章");

        if (novel.getWorkspaceDir() != null) {
            String mdDir = markdownStorage.initChapterDir(novel.getWorkspaceDir(), nextNumber);
            chapter.setMdDir(mdDir);
        }

        chapterMapper.insert(chapter);
        chapter.setId(chapterMapper.getLastInsertId());
        return chapter;
    }

    @Override
    public void updateChapter(Long id, Chapter chapter) {
        chapter.setId(id);
        Chapter existing = chapterMapper.selectById(id);
        if (existing != null && existing.getMdDir() != null) {
            Novel novel = novelService.getById(existing.getNovelId());
            if (novel != null && novel.getWorkspaceDir() != null) {
                String workspace = novel.getWorkspaceDir();
                String mdDir = existing.getMdDir();
                if (chapter.getSummary() != null) {
                    markdownStorage.writeChapterSummary(workspace, mdDir, chapter.getSummary());
                }
                if (chapter.getContent() != null) {
                    markdownStorage.writeChapterContent(workspace, mdDir, chapter.getContent());
                }
            }
        }
        chapterMapper.updateById(chapter);
    }

    @Override
    public void deleteChapter(Long id) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter != null && chapter.getMdDir() != null) {
            Novel novel = novelService.getById(chapter.getNovelId());
            if (novel != null && novel.getWorkspaceDir() != null) {
                markdownStorage.deleteChapterDir(novel.getWorkspaceDir(), chapter.getMdDir());
            }
        }
        chapterMapper.deleteById(id);
    }
}
