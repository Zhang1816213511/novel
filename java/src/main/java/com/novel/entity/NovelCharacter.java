package com.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("novel_character")
public class NovelCharacter {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long novelId;
    private String name;
    private String alias;
    private String description;
    private String personality;
    private String appearance;
    private String background;
    private String imagePath;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
