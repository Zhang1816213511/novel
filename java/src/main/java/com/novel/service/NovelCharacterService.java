package com.novel.service;

import com.novel.entity.NovelCharacter;

import java.util.List;

public interface NovelCharacterService {

    List<NovelCharacter> listByNovelId(Long novelId);

    NovelCharacter createCharacter(NovelCharacter character);

    NovelCharacter updateCharacter(Long id, NovelCharacter character);

    void deleteCharacter(Long id);
}
