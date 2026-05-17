package com.novel.controller;

import com.novel.common.Result;
import com.novel.dto.GenRequest;
import com.novel.service.NovelGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
@Tag(name = "小说生成", description = "多阶段小说生成（简介→大纲→章节梗概→正文）")
public class GenerationController {

    private final NovelGenerationService generationService;

    // ========== Step-by-step generation ==========

    @PostMapping("/{novelId}/synopsis")
    @Operation(summary = "生成作品简介")
    public Result<String> generateSynopsis(@PathVariable Long novelId, @RequestBody GenRequest req) {
        String synopsis = generationService.generateSynopsis(novelId, req.getModelName());
        return Result.success(synopsis);
    }

    @PostMapping("/{novelId}/outline")
    @Operation(summary = "生成作品大纲")
    public Result<String> generateOutline(@PathVariable Long novelId, @RequestBody GenRequest req) {
        String outline = generationService.generateOutline(novelId, req.getModelName());
        return Result.success(outline);
    }

    @PostMapping("/{novelId}/chapter-summary/{chapterId}")
    @Operation(summary = "生成章节梗概")
    public Result<String> generateChapterSummary(@PathVariable Long novelId,
                                                  @PathVariable Long chapterId,
                                                  @RequestBody GenRequest req) {
        String summary = generationService.generateChapterSummary(novelId, chapterId, req.getModelName());
        return Result.success(summary);
    }

    @PostMapping("/{novelId}/chapter-content/{chapterId}")
    @Operation(summary = "生成章节正文")
    public Result<String> generateChapterContent(@PathVariable Long novelId,
                                                  @PathVariable Long chapterId,
                                                  @RequestBody GenRequest req) {
        String content = generationService.generateChapterContent(novelId, chapterId, req.getModelName());
        return Result.success(content);
    }
}
