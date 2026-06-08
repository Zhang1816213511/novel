package com.novel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NovelDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;
}