package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.ModelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ModelConfigMapper extends BaseMapper<ModelConfig> {

    @Select("SELECT last_insert_rowid()")
    Long getLastInsertId();
}