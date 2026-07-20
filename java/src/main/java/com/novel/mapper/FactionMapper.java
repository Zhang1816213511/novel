package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.Faction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FactionMapper extends BaseMapper<Faction> {

    @Select("SELECT * FROM faction WHERE novel_id = #{novelId} ORDER BY id ASC")
    List<Faction> listByNovelId(Long novelId);

    @Select("SELECT last_insert_rowid()")
    Long getLastInsertId();
}
