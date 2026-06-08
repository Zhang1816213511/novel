package com.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    private String workspaceDir;

    /** 创建时指定根目录，后端自动以标题建子目录（不持久化） */
    @TableField(exist = false)
    private String rootDir;

    @TableField(exist = false)
    private String synopsis;

    @TableField(exist = false)
    private String outline;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}