package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.FactionRelation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FactionRelationMapper extends BaseMapper<FactionRelation> {

    @Select("SELECT * FROM faction_relation WHERE novel_id = #{novelId} ORDER BY id ASC")
    List<FactionRelation> listByNovelId(Long novelId);

    @Delete("DELETE FROM faction_relation WHERE source_faction_id = #{factionId} OR target_faction_id = #{factionId}")
    void deleteByFactionId(Long factionId);

    @Select("SELECT last_insert_rowid()")
    Long getLastInsertId();
}
