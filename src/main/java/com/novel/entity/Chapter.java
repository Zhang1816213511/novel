package com.novel.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chapter")
public class Chapter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long novelId;
    private Integer chapterNumber;
    private String title;
    private String summary;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
