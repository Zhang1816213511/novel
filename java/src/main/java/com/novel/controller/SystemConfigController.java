package com.novel.controller;

import com.novel.common.Result;
import com.novel.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "全局系统配置管理")
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/workspace-root")
    @Operation(summary = "获取工作区根目录")
    public Result<String> getWorkspaceRoot() {
        return Result.success(systemConfigService.getWorkspaceRoot());
    }

    @PutMapping("/workspace-root")
    @Operation(summary = "设置工作区根目录")
    public Result<Void> setWorkspaceRoot(@RequestBody Map<String, String> body) {
        String root = body.get("workspaceRoot");
        if (root == null || root.isBlank()) {
            return Result.validateFailed("工作目录路径不能为空");
        }
        systemConfigService.setWorkspaceRoot(root);
        return Result.success();
    }

    @GetMapping("/status")
    @Operation(summary = "获取系统配置状态（是否已配置工作目录）")
    public Result<Map<String, Object>> getStatus() {
        String root = systemConfigService.getWorkspaceRoot();
        Map<String, Object> status = new HashMap<>();
        status.put("configured", root != null);
        status.put("workspaceRoot", root);
        return Result.success(status);
    }
}
