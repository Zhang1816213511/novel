package com.novel.service;

import com.novel.entity.Faction;

import java.util.List;

public interface FactionService {
    List<Faction> listByNovelId(Long novelId);
    Faction createFaction(Faction faction);
    Faction updateFaction(Long id, Faction faction);
    void deleteFaction(Long id);
}
