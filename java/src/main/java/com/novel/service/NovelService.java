package com.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.novel.entity.Novel;
import com.novel.mapper.NovelMapper;
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
public class NovelService extends ServiceImpl<NovelMapper, Novel> {

    private final NovelMapper novelMapper;
    private final MarkdownStorageService markdownStorage;
    private final SystemConfigService systemConfigService;

    // Caffeine 本地缓存
    private final Cache<Long, Novel> novelCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .recordStats()
            .build();

    @Transactional(rollbackFor = Exception.class)
    public Novel saveNovel(Novel novel) {
        // 如果没有指定目录，从全局工作目录自动创建
        if (novel.getWorkspaceDir() == null || novel.getWorkspaceDir().isBlank()) {
            String root = systemConfigService.getWorkspaceRoot();
            if (root == null) {
                throw new IllegalArgumentException("请先在系统设置中配置工作区根目录");
            }
            String dirName = sanitizeForDirectoryName(novel.getTitle());
            novel.setWorkspaceDir(root + "/" + dirName);
        }
        // 初始化作品目录
        markdownStorage.initWorkspace(novel.getWorkspaceDir());
        novelMapper.insert(novel);
        novel.setId(novelMapper.getLastInsertId());
        return novel;
    }

    /**
     * 将作品标题安全转换为文件夹名称。
     * 替换 Windows 非法文件名字符，去除首尾下划线，避免空名称。
     */
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

    public List<Novel> listAll() {
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Novel::getUpdateTime);
        // 列表不加载文件内容（synopsis/outline 由 @TableField(exist=false) 控制）
        return novelMapper.selectList(wrapper);
    }

    @Cacheable(value = "novels", key = "#id")
    public Novel getById(Long id) {
        Novel novel = novelMapper.selectById(id);
        if (novel != null) {
            // 从文件加载简介和大纲
            if (novel.getWorkspaceDir() != null) {
                novel.setSynopsis(markdownStorage.readSynopsis(novel.getWorkspaceDir()));
                novel.setOutline(markdownStorage.readOutline(novel.getWorkspaceDir()));
            }
            novelCache.put(id, novel);
        }
        return novel;
    }

    public Novel updateNovel(Long id, Novel novel) {
        novel.setId(id);
        novelMapper.updateById(novel);
        novelCache.invalidate(id);
        // 同步写入文件
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

    public void deleteById(Long id) {
        Novel novel = novelMapper.selectById(id);
        novelMapper.deleteById(id);
        novelCache.invalidate(id);
        // 不自动删除工作目录文件（防止误删）
        if (novel != null && novel.getWorkspaceDir() != null) {
            log.info("作品已删除，工作目录保留: {}", novel.getWorkspaceDir());
        }
    }

    public String getCacheStats() {
        return String.format("Hits: %d, Misses: %d",
                novelCache.stats().hitCount(),
                novelCache.stats().missCount());
    }
}