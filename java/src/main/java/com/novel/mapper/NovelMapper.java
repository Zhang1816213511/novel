package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.Novel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NovelMapper extends BaseMapper<Novel> {

    @Select("SELECT last_insert_rowid()")
    Long getLastInsertId();
}