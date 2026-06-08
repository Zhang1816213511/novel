package com.novel.dto;

import com.novel.enums.GenStage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GenerationTask {
    private String taskId;
    private Long novelId;
    private GenStage stage = GenStage.SYNOPSIS;
    private int progress;
    private String error;
    private boolean done;
    private List<StageInfo> stages = new ArrayList<>();

    @Data
    public static class StageInfo {
        private GenStage stage;
        private String status; // "waiting", "running", "done", "error"
        private String message;
    }
}
