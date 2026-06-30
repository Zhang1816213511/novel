package com.novel.service;

public interface MarkdownStorageService {

    void initWorkspace(String workspaceDir);

    String readSynopsis(String workspaceDir);

    void writeSynopsis(String workspaceDir, String content);

    String readOutline(String workspaceDir);

    void writeOutline(String workspaceDir, String content);

    String initChapterDir(String workspaceDir, int chapterNumber);

    String readChapterSummary(String workspaceDir, String chapterDir);

    void writeChapterSummary(String workspaceDir, String chapterDir, String content);

    String readChapterContent(String workspaceDir, String chapterDir);

    void writeChapterContent(String workspaceDir, String chapterDir, String content);

    void deleteChapterDir(String workspaceDir, String chapterDir);
}
