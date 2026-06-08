package com.novel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.novel.entity.SystemConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    @Select("SELECT * FROM system_config WHERE config_key = #{key}")
    SystemConfig selectByKey(@Param("key") String key);

    /**
     * INSERT OR REPLACE 是 SQLite 的 upsert 写法。
     * COALESCE 子查询保留原有 create_time，避免每次更新时覆盖。
     */
    @Insert("INSERT OR REPLACE INTO system_config " +
            "(config_key, config_value, create_time, update_time) " +
            "VALUES (#{configKey}, #{configValue}, " +
            "COALESCE((SELECT create_time FROM system_config WHERE config_key = #{configKey}), datetime('now')), " +
            "datetime('now'))")
    void upsertConfig(@Param("configKey") String configKey, @Param("configValue") String configValue);
}
