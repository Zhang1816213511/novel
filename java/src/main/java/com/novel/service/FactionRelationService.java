package com.novel.service;

import com.novel.entity.FactionRelation;

import java.util.List;

public interface FactionRelationService {
    List<FactionRelation> listByNovelId(Long novelId);
    FactionRelation createRelation(FactionRelation relation);
    FactionRelation updateRelation(Long id, FactionRelation relation);
    void deleteRelation(Long id);
    void deleteByFactionId(Long factionId);
}
