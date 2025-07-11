package com.dingtalk.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dingtalk.alert.entity.DatabaseConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据库配置Mapper接口
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface DatabaseConfigMapper extends BaseMapper<DatabaseConfig> {

    /**
     * 查询所有启用的数据库配置
     * 
     * @return 数据库配置列表
     */
    @Select("SELECT * FROM database_config WHERE enabled = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<DatabaseConfig> selectEnabledConfigs();

    /**
     * 根据配置名称查询
     * 
     * @param configName 配置名称
     * @return 数据库配置
     */
    @Select("SELECT * FROM database_config WHERE config_name = #{configName} AND deleted = 0")
    DatabaseConfig selectByConfigName(String configName);
}