package com.novel.dto;

import lombok.Data;

@Data
public class GenerationRequest {
    private int chapters = 5;
    private int summaryLength = 200;
    private String modelName;
}
