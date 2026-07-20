package com.novel.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.novel.entity.Faction;
import com.novel.entity.NovelCharacter;
import com.novel.mapper.FactionMapper;
import com.novel.mapper.NovelCharacterMapper;
import com.novel.service.NovelCharacterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NovelCharacterServiceImpl extends ServiceImpl<NovelCharacterMapper, NovelCharacter> implements NovelCharacterService {

    private final NovelCharacterMapper characterMapper;
    private final FactionMapper factionMapper;

    @Override
    public List<NovelCharacter> listByNovelId(Long novelId) {
        List<NovelCharacter> chars = characterMapper.listByNovelId(novelId);
        Set<Long> factionIds = chars.stream()
                .map(NovelCharacter::getFactionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!factionIds.isEmpty()) {
            Map<Long, String> factionMap = factionMapper.selectBatchIds(factionIds).stream()
                    .collect(Collectors.toMap(Faction::getId, Faction::getName));
            chars.forEach(c -> c.setFactionName(factionMap.get(c.getFactionId())));
        }
        return chars;
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
