package com.novel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long novelId;
    private String role;
    private String content;
    private String refs;
    private String changes;
    private LocalDateTime createTime;
}
