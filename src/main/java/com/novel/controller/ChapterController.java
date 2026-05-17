package com.novel.controller;

import com.novel.common.Result;
import com.novel.entity.Chapter;
import com.novel.service.ChapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chapters")
@RequiredArgsConstructor
@Tag(name = "章节管理")
public class ChapterController {

    private final ChapterService chapterService;

    @GetMapping("/novel/{novelId}")
    @Operation(summary = "获取作品的所有章节")
    public Result<List<Chapter>> listByNovel(@PathVariable Long novelId) {
        return Result.success(chapterService.listByNovelId(novelId));
    }

    @PostMapping("/novel/{novelId}")
    @Operation(summary = "创建新章节")
    public Result<Chapter> create(@PathVariable Long novelId) {
        return Result.success(chapterService.createChapter(novelId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新章节")
    public Result<Chapter> update(@PathVariable Long id, @RequestBody Chapter chapter) {
        chapter.setId(id);
        chapterService.updateById(chapter);
        return Result.success(chapterService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除章节")
    public Result<Void> delete(@PathVariable Long id) {
        chapterService.removeById(id);
        return Result.success();
    }
}
