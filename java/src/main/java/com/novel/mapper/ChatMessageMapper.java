package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT * FROM chat_message WHERE novel_id = #{novelId} ORDER BY id ASC")
    List<ChatMessage> listByNovelId(Long novelId);

    @Delete("DELETE FROM chat_message WHERE novel_id = #{novelId}")
    void deleteByNovelId(Long novelId);

    @Select("SELECT last_insert_rowid()")
    Long getLastInsertId();
}
