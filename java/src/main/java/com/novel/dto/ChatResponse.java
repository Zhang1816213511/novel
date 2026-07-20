package com.novel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /** AI 回复文本 */
    private String reply;
    /** 实际发生的文件改动（旧格式，兼容） */
    private List<FileChange> changes;
    /** 工具执行后，当前作品的最新内容状态，前端据此刷新 UI */
    private UpdatedContent updated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileChange {
        private String type;
        private Integer chapterNumber;
        private String section;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatedContent {
        private String synopsis;
        private String outline;
        private List<ChapterState> chapters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterState {
        private Integer chapterNumber;
        private String title;
        private String summary;
        private String content;
    }
}
