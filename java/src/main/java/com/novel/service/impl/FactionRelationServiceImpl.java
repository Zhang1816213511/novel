package com.novel.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.novel.entity.FactionRelation;
import com.novel.mapper.FactionRelationMapper;
import com.novel.service.FactionRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FactionRelationServiceImpl extends ServiceImpl<FactionRelationMapper, FactionRelation> implements FactionRelationService {

    private final FactionRelationMapper relationMapper;

    @Override
    public List<FactionRelation> listByNovelId(Long novelId) {
        return relationMapper.listByNovelId(novelId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FactionRelation createRelation(FactionRelation relation) {
        relationMapper.insert(relation);
        relation.setId(relationMapper.getLastInsertId());
        return relation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FactionRelation updateRelation(Long id, FactionRelation relation) {
        relation.setId(id);
        relationMapper.updateById(relation);
        return relationMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRelation(Long id) {
        relationMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByFactionId(Long factionId) {
        relationMapper.deleteByFactionId(factionId);
    }
}
