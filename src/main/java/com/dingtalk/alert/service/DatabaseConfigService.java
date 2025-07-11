package com.dingtalk.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dingtalk.alert.entity.DatabaseConfig;
import com.dingtalk.alert.mapper.DatabaseConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据库配置服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DatabaseConfigService extends ServiceImpl<DatabaseConfigMapper, DatabaseConfig> {

    @Autowired
    private DatabaseService databaseService;

    /**
     * 保存数据库配置
     * 
     * @param config 数据库配置
     * @return 保存结果
     */
    @Override
    @Transactional
    public boolean save(DatabaseConfig config) {
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        config.setDeleted(false);
        return super.save(config);
    }

    /**
     * 更新数据库配置
     * 
     * @param config 数据库配置
     * @return 更新结果
     */
    @Override
    @Transactional
    public boolean updateById(DatabaseConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        return super.updateById(config);
    }

    /**
     * 获取启用的数据库配置
     * 
     * @return 数据库配置列表
     */
    public List<DatabaseConfig> getEnabledConfigs() {
        QueryWrapper<DatabaseConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("enabled", true)
               .eq("deleted", false)
               .orderByDesc("create_time");
        return list(wrapper);
    }

    /**
     * 根据配置名称查询
     * 
     * @param configName 配置名称
     * @return 数据库配置
     */
    public DatabaseConfig getByConfigName(String configName) {
        QueryWrapper<DatabaseConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_name", configName)
               .eq("deleted", false);
        return getOne(wrapper);
    }

    /**
     * 测试数据库连接
     * 
     * @param config 数据库配置
     * @return 测试结果
     */
    public boolean testConnection(DatabaseConfig config) {
        try {
            return databaseService.testConnection(config);
        } catch (Exception e) {
            log.error("测试数据库连接失败", e);
            return false;
        }
    }

    /**
     * 删除数据库配置（逻辑删除）
     * 
     * @param id 配置ID
     * @return 删除结果
     */
    @Transactional
    public boolean deleteConfig(Long id) {
        DatabaseConfig config = getById(id);
        if (config != null) {
            config.setDeleted(true);
            config.setUpdateTime(LocalDateTime.now());
            return removeById(config);
        }
        return false;
    }
}