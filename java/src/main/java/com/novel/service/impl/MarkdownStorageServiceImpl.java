package com.novel.service.impl;

import com.novel.service.MarkdownStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class MarkdownStorageServiceImpl implements MarkdownStorageService {

    private static final String SYNOPSIS_FILE = "synopsis.md";
    private static final String OUTLINE_FILE = "outline.md";
    private static final String SUMMARY_FILE = "summary.md";
    private static final String CONTENT_FILE = "content.md";
    private static final String CHAPTER_PREFIX = "ch_";

    @Override
    public void initWorkspace(String workspaceDir) {
        Path dir = Paths.get(workspaceDir);
        try {
            Files.createDirectories(dir);
            log.info("Workspace initialized: {}", dir.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("无法创建工作目录: " + workspaceDir, e);
        }
    }

    @Override
    public String readSynopsis(String workspaceDir) {
        return readFile(workspaceDir, SYNOPSIS_FILE);
    }

    @Override
    public void writeSynopsis(String workspaceDir, String content) {
        writeFile(workspaceDir, SYNOPSIS_FILE, content);
    }

    @Override
    public String readOutline(String workspaceDir) {
        return readFile(workspaceDir, OUTLINE_FILE);
    }

    @Override
    public void writeOutline(String workspaceDir, String content) {
        writeFile(workspaceDir, OUTLINE_FILE, content);
    }

    @Override
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

    @Override
    public String readChapterSummary(String workspaceDir, String chapterDir) {
        return readFile(workspaceDir, chapterDir, SUMMARY_FILE);
    }

    @Override
    public void writeChapterSummary(String workspaceDir, String chapterDir, String content) {
        writeFile(workspaceDir, chapterDir, SUMMARY_FILE, content);
    }

    @Override
    public String readChapterContent(String workspaceDir, String chapterDir) {
        return readFile(workspaceDir, chapterDir, CONTENT_FILE);
    }

    @Override
    public void writeChapterContent(String workspaceDir, String chapterDir, String content) {
        writeFile(workspaceDir, chapterDir, CONTENT_FILE, content);
    }

    @Override
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

    private static String chapterDirName(int chapterNumber) {
        return CHAPTER_PREFIX + String.format("%03d", chapterNumber);
    }

    private String readFile(String workspaceDir, String fileName) {
        Path file = Paths.get(workspaceDir, fileName);
        if (!Files.exists(file)) return "";
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("读取文件失败: {}", file, e);
            return "";
        }
    }

    private String readFile(String workspaceDir, String subDir, String fileName) {
        Path file = Paths.get(workspaceDir, subDir, fileName);
        if (!Files.exists(file)) return "";
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
