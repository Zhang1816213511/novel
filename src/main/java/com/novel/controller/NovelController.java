package com.novel.controller;

import com.novel.common.Result;
import com.novel.entity.Novel;
import com.novel.service.NovelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/novels")
@RequiredArgsConstructor
@Tag(name = "作品管理", description = "小说/写作项目管理接口")
public class NovelController {

    @Autowired
    private NovelService novelService;

    @PostMapping
    @Operation(summary = "新建作品")
    public Result<Novel> create(@RequestBody Novel novel) {
        return Result.success(novelService.saveNovel(novel));
    }

    @GetMapping
    @Operation(summary = "获取作品列表")
    public Result<List<Novel>> list() {
        return Result.success(novelService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单篇作品")
    public Result<Novel> getById(@PathVariable Long id) {
        return Result.success(novelService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新作品")
    public Result<Novel> update(@PathVariable Long id, @RequestBody Novel novel) {
        return Result.success(novelService.updateNovel(id, novel));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除作品")
    public Result<Void> delete(@PathVariable Long id) {
        novelService.deleteById(id);
        return Result.success();
    }
}