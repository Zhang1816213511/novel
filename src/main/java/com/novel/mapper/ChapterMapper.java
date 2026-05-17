package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.Chapter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChapterMapper extends BaseMapper<Chapter> {

    @Select("SELECT * FROM chapter WHERE novel_id = #{novelId} ORDER BY chapter_number ASC")
    List<Chapter> listByNovelId(Long novelId);

    @Select("SELECT COALESCE(MAX(chapter_number), 0) FROM chapter WHERE novel_id = #{novelId}")
    Integer getMaxChapterNumber(Long novelId);
}
