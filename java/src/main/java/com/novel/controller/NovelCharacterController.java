package com.novel.controller;

import com.novel.common.Result;
import com.novel.entity.NovelCharacter;
import com.novel.service.NovelCharacterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Tag(name = "角色管理", description = "作品角色管理接口")
public class NovelCharacterController {

    private final NovelCharacterService characterService;

    @GetMapping("/novel/{novelId}")
    @Operation(summary = "获取作品的所有角色")
    public Result<List<NovelCharacter>> listByNovel(@PathVariable Long novelId) {
        return Result.success(characterService.listByNovelId(novelId));
    }

    @PostMapping
    @Operation(summary = "新增角色")
    public Result<NovelCharacter> create(@RequestBody NovelCharacter character) {
        return Result.success(characterService.createCharacter(character));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    public Result<NovelCharacter> update(@PathVariable Long id, @RequestBody NovelCharacter character) {
        return Result.success(characterService.updateCharacter(id, character));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public Result<Void> delete(@PathVariable Long id) {
        characterService.deleteCharacter(id);
        return Result.success();
    }
}
