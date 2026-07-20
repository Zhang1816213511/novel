package com.novel.controller;

import com.novel.common.Result;
import com.novel.entity.Faction;
import com.novel.entity.FactionRelation;
import com.novel.service.FactionRelationService;
import com.novel.service.FactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/factions")
@RequiredArgsConstructor
@Tag(name = "势力管理", description = "势力 CRUD 及关系管理")
public class FactionController {

    private final FactionService factionService;
    private final FactionRelationService relationService;

    // ─── 势力 CRUD ───

    @GetMapping("/novel/{novelId}")
    @Operation(summary = "获取作品所有势力")
    public Result<java.util.List<Faction>> listFactions(@PathVariable Long novelId) {
        return Result.success(factionService.listByNovelId(novelId));
    }

    @PostMapping
    @Operation(summary = "创建势力")
    public Result<Faction> createFaction(@RequestBody Faction faction) {
        return Result.success(factionService.createFaction(faction));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新势力")
    public Result<Faction> updateFaction(@PathVariable Long id, @RequestBody Faction faction) {
        return Result.success(factionService.updateFaction(id, faction));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除势力（级联删除关系和角色关联）")
    public Result<Void> deleteFaction(@PathVariable Long id) {
        factionService.deleteFaction(id);
        return Result.success();
    }

    // ─── 势力关系 CRUD ───

    @GetMapping("/{novelId}/relations")
    @Operation(summary = "获取作品所有势力关系")
    public Result<java.util.List<FactionRelation>> listRelations(@PathVariable Long novelId) {
        return Result.success(relationService.listByNovelId(novelId));
    }

    @PostMapping("/relations")
    @Operation(summary = "创建势力关系")
    public Result<FactionRelation> createRelation(@RequestBody FactionRelation relation) {
        return Result.success(relationService.createRelation(relation));
    }

    @PutMapping("/relations/{id}")
    @Operation(summary = "更新势力关系")
    public Result<FactionRelation> updateRelation(@PathVariable Long id, @RequestBody FactionRelation relation) {
        return Result.success(relationService.updateRelation(id, relation));
    }

    @DeleteMapping("/relations/{id}")
    @Operation(summary = "删除势力关系")
    public Result<Void> deleteRelation(@PathVariable Long id) {
        relationService.deleteRelation(id);
        return Result.success();
    }

    // ─── 图谱数据 ───

    @GetMapping("/{novelId}/graph")
    @Operation(summary = "获取势力图谱数据（节点+边）")
    public Result<Map<String, Object>> getGraph(@PathVariable Long novelId) {
        return Result.success(Map.of(
                "factions", factionService.listByNovelId(novelId),
                "relations", relationService.listByNovelId(novelId)
        ));
    }
}
