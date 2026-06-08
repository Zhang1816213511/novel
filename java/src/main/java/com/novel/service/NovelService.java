package com.novel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.novel.entity.Novel;
import com.novel.mapper.NovelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class NovelService extends ServiceImpl<NovelMapper, Novel> {

    private final NovelMapper novelMapper;

    // Caffeine 本地缓存
    private final Cache<Long, Novel> novelCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .recordStats()
            .build();

    @Transactional(rollbackFor = Exception.class)
    public Novel saveNovel(Novel novel) {
        novelMapper.insert(novel);
        novel.setId(novelMapper.getLastInsertId());
        return novel;
    }

    public List<Novel> listAll() {
        LambdaQueryWrapper<Novel> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Novel::getUpdateTime);
        return novelMapper.selectList(wrapper);
    }

    @Cacheable(value = "novels", key = "#id")
    public Novel getById(Long id) {
        Novel novel = novelMapper.selectById(id);
        if (novel != null) {
            novelCache.put(id, novel);
        }
        return novel;
    }

    public Novel updateNovel(Long id, Novel novel) {
        novel.setId(id);
        novelMapper.updateById(novel);
        novelCache.invalidate(id);
        return novel;
    }

    public void deleteById(Long id) {
        novelMapper.deleteById(id);
        novelCache.invalidate(id);
    }

    public String getCacheStats() {
        return String.format("Hits: %d, Misses: %d",
                novelCache.stats().hitCount(),
                novelCache.stats().missCount());
    }
}