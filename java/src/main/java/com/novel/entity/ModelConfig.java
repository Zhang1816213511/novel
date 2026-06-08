package com.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("model_config")
public class ModelConfig {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String name;

    private String provider;

    private String baseUrl;

    private String modelName;

    private String apiKey;

    private String options;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}