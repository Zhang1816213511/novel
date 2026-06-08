package com.novel.controller;

import com.novel.common.Result;
import com.novel.entity.ModelConfig;
import com.novel.service.ModelConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
@Tag(name = "模型管理", description = "AI 模型配置管理接口")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    @GetMapping
    @Operation(summary = "获取模型列表")
    public Result<List<ModelConfig>> list() {
        return Result.success(modelConfigService.listAll());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取已启用的模型列表")
    public Result<List<ModelConfig>> listEnabled() {
        return Result.success(modelConfigService.listEnabled());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个模型")
    public Result<ModelConfig> getById(@PathVariable Long id) {
        return Result.success(modelConfigService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增模型")
    public Result<ModelConfig> create(@RequestBody ModelConfig modelConfig) {
        return Result.success(modelConfigService.saveModel(modelConfig));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新模型")
    public Result<ModelConfig> update(@PathVariable Long id, @RequestBody ModelConfig modelConfig) {
        return Result.success(modelConfigService.updateModel(id, modelConfig));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除模型")
    public Result<Void> delete(@PathVariable Long id) {
        modelConfigService.deleteById(id);
        return Result.success();
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用模型")
    public Result<ModelConfig> toggle(@PathVariable Long id) {
        return Result.success(modelConfigService.toggleEnabled(id));
    }
}