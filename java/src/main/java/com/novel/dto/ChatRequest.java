package com.novel.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    /** 用户消息 */
    private String message;
    /** 引用的文件 */
    private List<FileRef> refs;
    /** 模型名称 */
    private String modelName;

    @Data
    public static class FileRef {
        private String type;       // "synopsis" | "outline" | "chapter"
        private Integer chapterNumber; // 章节号（当 type=chapter 时）
        private String section;    // "summary" | "content"（当 type=chapter 时）
        private Integer startLine;
        private Integer endLine;
    }
}
