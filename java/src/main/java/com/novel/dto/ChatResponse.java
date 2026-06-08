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
    /** 实际发生的文件改动 */
    private List<FileChange> changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileChange {
        private String type;          // "synopsis" | "outline" | "chapter"
        private Integer chapterNumber;
        private String section;       // "summary" | "content"
        private String content;       // 修改后的完整内容
    }
}
