package com.novel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novel.entity.NovelCharacter;
import com.novel.mapper.NovelCharacterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NovelCharacterService extends ServiceImpl<NovelCharacterMapper, NovelCharacter> {

    private final NovelCharacterMapper characterMapper;

    public List<NovelCharacter> listByNovelId(Long novelId) {
        return characterMapper.listByNovelId(novelId);
    }

    @Transactional(rollbackFor = Exception.class)
    public NovelCharacter createCharacter(NovelCharacter character) {
        characterMapper.insert(character);
        character.setId(characterMapper.getLastInsertId());
        return character;
    }

    @Transactional(rollbackFor = Exception.class)
    public NovelCharacter updateCharacter(Long id, NovelCharacter character) {
        character.setId(id);
        characterMapper.updateById(character);
        return characterMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteCharacter(Long id) {
        characterMapper.deleteById(id);
    }
}
