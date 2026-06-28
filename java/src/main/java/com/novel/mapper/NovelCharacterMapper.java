package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.NovelCharacter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NovelCharacterMapper extends BaseMapper<NovelCharacter> {

    @Select("SELECT * FROM novel_character WHERE novel_id = #{novelId} ORDER BY sort_order ASC, id ASC")
    List<NovelCharacter> listByNovelId(Long novelId);

    @Select("SELECT last_insert_rowid()")
    Long getLastInsertId();
}
