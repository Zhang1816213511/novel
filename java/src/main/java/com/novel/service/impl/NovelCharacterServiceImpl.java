package com.novel.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.novel.entity.NovelCharacter;
import com.novel.mapper.NovelCharacterMapper;
import com.novel.service.NovelCharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NovelCharacterServiceImpl extends ServiceImpl<NovelCharacterMapper, NovelCharacter> implements NovelCharacterService {

    private final NovelCharacterMapper characterMapper;

    @Override
    public List<NovelCharacter> listByNovelId(Long novelId) {
        return characterMapper.listByNovelId(novelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelCharacter createCharacter(NovelCharacter character) {
        characterMapper.insert(character);
        character.setId(characterMapper.getLastInsertId());
        return character;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelCharacter updateCharacter(Long id, NovelCharacter character) {
        character.setId(id);
        characterMapper.updateById(character);
        return characterMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCharacter(Long id) {
        characterMapper.deleteById(id);
    }
}
