package com.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("novel_entity")
public class Novel {

    @TableId(type = IdType.INPUT)
    private Long id;

    private String title;

    private String content;

    private String synopsis;

    private String outline;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}