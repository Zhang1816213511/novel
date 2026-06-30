package com.novel.service;

import com.novel.entity.Novel;

import java.util.List;

public interface NovelService {

    Novel saveNovel(Novel novel);

    List<Novel> listAll();

    Novel getById(Long id);

    Novel updateNovel(Long id, Novel novel);

    void deleteById(Long id);

    String getCacheStats();
}
