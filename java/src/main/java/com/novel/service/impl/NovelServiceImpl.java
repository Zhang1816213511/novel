package com.novel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.novel.entity.Novel;
import com.novel.mapper.NovelMapper;
import com.novel.service.MarkdownStorageService;
import com.novel.service.NovelService;
import com.novel.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelServiceImpl extends ServiceImpl<NovelMapper, Novel> implements NovelService {

    private final NovelMapper novelMapper;
    private final MarkdownStorageService markdownStorage;
    private final SystemConfigService systemConfigService;

    private final Cache<Long, Novel> novelCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .recordStats()
            .build();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Novel saveNovel(Novel novel) {
        if (novel.getWorkspaceDir() == null || novel.getWorkspaceDir().isBlank()) {
            String root = systemConfigService.getWorkspaceRoot();
            if (root == null) {
                throw new IllegalArgumentException("请先在系统设置中配置工作区根目录");
            }
            String dirName = sanitizeForDirectoryName(novel.getTitle());
            novel.setWorkspaceDir(root + "/" + dirName);
        }
        markdownStorage.initWorkspace(novel.getWorkspaceDir());
        novelMapper.insert(novel);
        novel.setId(novelMapper.getLastInsertId());
        return novel;
    }

    public static String sanitizeForDirectoryName(String title) {
        if (title == null || title.isBlank()) return "untitled";
        String name = title
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("^_+|_+$", "")
                .trim();
        if (name.isEmpty()) {
            name = "novel_" + System.currentTimeMillis();
        }
        return name;
    }

    @Override
    public List<Novel> listAll() {
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Novel::getUpdateTime);
        return novelMapper.selectList(wrapper);
    }

    @Override
    @Cacheable(value = "novels", key = "#id")
    public Novel getById(Long id) {
        Novel novel = novelMapper.selectById(id);
        if (novel != null) {
            if (novel.getWorkspaceDir() != null) {
                novel.setSynopsis(markdownStorage.readSynopsis(novel.getWorkspaceDir()));
                novel.setOutline(markdownStorage.readOutline(novel.getWorkspaceDir()));
            }
            novelCache.put(id, novel);
        }
        return novel;
    }

    @Override
    public Novel updateNovel(Long id, Novel novel) {
        novel.setId(id);
        novelMapper.updateById(novel);
        novelCache.invalidate(id);
        if (novel.getWorkspaceDir() != null) {
            if (novel.getSynopsis() != null) {
                markdownStorage.writeSynopsis(novel.getWorkspaceDir(), novel.getSynopsis());
            }
            if (novel.getOutline() != null) {
                markdownStorage.writeOutline(novel.getWorkspaceDir(), novel.getOutline());
            }
        }
        return novel;
    }

    @Override
    public void deleteById(Long id) {
        Novel novel = novelMapper.selectById(id);
        novelMapper.deleteById(id);
        novelCache.invalidate(id);
        if (novel != null && novel.getWorkspaceDir() != null) {
            log.info("作品已删除，工作目录保留: {}", novel.getWorkspaceDir());
        }
    }

    @Override
    public String getCacheStats() {
        return String.format("Hits: %d, Misses: %d",
                novelCache.stats().hitCount(),
                novelCache.stats().missCount());
    }
}
