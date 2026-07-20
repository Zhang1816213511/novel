package com.novel.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.novel.entity.Faction;
import com.novel.mapper.FactionMapper;
import com.novel.mapper.FactionRelationMapper;
import com.novel.mapper.NovelCharacterMapper;
import com.novel.service.FactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactionServiceImpl extends ServiceImpl<FactionMapper, Faction> implements FactionService {

    private final FactionMapper factionMapper;
    private final FactionRelationMapper relationMapper;
    private final NovelCharacterMapper characterMapper;

    @Override
    public List<Faction> listByNovelId(Long novelId) {
        return factionMapper.listByNovelId(novelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Faction createFaction(Faction faction) {
        factionMapper.insert(faction);
        faction.setId(factionMapper.getLastInsertId());
        return faction;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Faction updateFaction(Long id, Faction faction) {
        faction.setId(id);
        factionMapper.updateById(faction);
        return factionMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFaction(Long id) {
        relationMapper.deleteByFactionId(id);
        characterMapper.clearFactionId(id);
        factionMapper.deleteById(id);
    }
}
