package com.novel.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 作品内容 Markdown 文件存储服务。
 * <p>
 * 目录结构：
 * <pre>
 * {workspaceDir}/
 *   ├── synopsis.md
 *   ├── outline.md
 *   ├── ch_001/
 *   │   ├── summary.md
 *   │   └── content.md
 *   ├── ch_002/
 *   │   ├── summary.md
 *   │   └── content.md
 *   └── ...
 * </pre>
 */
@Slf4j
@Service
public class MarkdownStorageService {

    private static final String SYNOPSIS_FILE = "synopsis.md";
    private static final String OUTLINE_FILE = "outline.md";
    private static final String SUMMARY_FILE = "summary.md";
    private static final String CONTENT_FILE = "content.md";
    private static final String CHAPTER_PREFIX = "ch_";

    // ========== Workspace 初始化 ==========

    /**
     * 初始化作品目录结构
     */
    public void initWorkspace(String workspaceDir) {
        Path dir = Paths.get(workspaceDir);
        try {
            Files.createDirectories(dir);
            log.info("Workspace initialized: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建工作目录: " + workspaceDir, e);
        }
    }

    // ========== 简介 ==========

    public String readSynopsis(String workspaceDir) {
        return readFile(workspaceDir, SYNOPSIS_FILE);
    }

    public void writeSynopsis(String workspaceDir, String content) {
        writeFile(workspaceDir, SYNOPSIS_FILE, content);
    }

    // ========== 大纲 ==========

    public String readOutline(String workspaceDir) {
        return readFile(workspaceDir, OUTLINE_FILE);
    }

    public void writeOutline(String workspaceDir, String content) {
        writeFile(workspaceDir, OUTLINE_FILE, content);
    }

    // ========== 章节 ==========

    /**
     * 初始化章节目录并返回 mdDir（相对路径，如 "ch_001"）
     */
    public String initChapterDir(String workspaceDir, int chapterNumber) {
        String chapterDir = chapterDirName(chapterNumber);
        Path dir = Paths.get(workspaceDir, chapterDir);
        try {
            Files.createDirectories(dir);
            log.info("Chapter dir created: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建章节目录: " + dir, e);
        }
        return chapterDir;
    }

    public String readChapterSummary(String workspaceDir, String chapterDir) {
        return readFile(workspaceDir, chapterDir, SUMMARY_FILE);
    }

    public void writeChapterSummary(String workspaceDir, String chapterDir, String content) {
        writeFile(workspaceDir, chapterDir, SUMMARY_FILE, content);
    }

    public String readChapterContent(String workspaceDir, String chapterDir) {
        return readFile(workspaceDir, chapterDir, CONTENT_FILE);
    }

    public void writeChapterContent(String workspaceDir, String chapterDir, String content) {
        writeFile(workspaceDir, chapterDir, CONTENT_FILE, content);
    }

    /**
     * 删除章节目录及其所有文件
     */
    public void deleteChapterDir(String workspaceDir, String chapterDir) {
        Path dir = Paths.get(workspaceDir, chapterDir);
        try {
            if (Files.exists(dir)) {
                try (var files = Files.walk(dir)) {
                    files.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                            });
                }
                log.info("Chapter dir deleted: {}", dir);
            }
        } catch (IOException e) {
            log.warn("删除章节目录失败: {}", dir, e);
        }
    }

    // ========== 工具方法 ==========

    private static String chapterDirName(int chapterNumber) {
        return CHAPTER_PREFIX + String.format("%03d", chapterNumber);
    }

    private String readFile(String workspaceDir, String fileName) {
        Path file = Paths.get(workspaceDir, fileName);
        if (!Files.exists(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取文件失败: {}", file, e);
            return "";
        }
    }

    private String readFile(String workspaceDir, String subDir, String fileName) {
        Path file = Paths.get(workspaceDir, subDir, fileName);
        if (!Files.exists(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取文件失败: {}", file, e);
            return "";
        }
    }

    private void writeFile(String workspaceDir, String fileName, String content) {
        Path file = Paths.get(workspaceDir, fileName);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content != null ? content : "", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }

    private void writeFile(String workspaceDir, String subDir, String fileName, String content) {
        Path file = Paths.get(workspaceDir, subDir, fileName);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content != null ? content : "", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + file, e);
        }
    }
}
