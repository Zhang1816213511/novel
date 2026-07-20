package com.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("faction_relation")
public class FactionRelation {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long novelId;
    private Long sourceFactionId;
    private Long targetFactionId;
    private String relationType;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
