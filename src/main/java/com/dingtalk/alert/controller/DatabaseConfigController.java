package com.dingtalk.alert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.alert.entity.DatabaseConfig;
import com.dingtalk.alert.service.DatabaseConfigService;
import com.dingtalk.alert.service.DatabaseService;
import com.dingtalk.alert.dto.DatabaseConfigListDTO;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库配置控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/database")
public class DatabaseConfigController {

    @Autowired
    private DatabaseConfigService databaseConfigService;

    @Autowired
    private DatabaseService databaseService;
    
    // 简单的内存缓存，避免频繁查询
    private volatile Long cachedCount = null;
    private volatile long lastCountCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存

    /**
     * 获取数据库配置总数
     */
    @GetMapping("/count")
    public Map<String, Object> count() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用缓存优化性能
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCountCacheTime < CACHE_DURATION && cachedCount != null) {
                result.put("success", true);
                result.put("data", cachedCount);
                return result;
            }
            
            long count = databaseConfigService.count();
            cachedCount = count;
            lastCountCacheTime = currentTime;
            
            result.put("success", true);
            result.put("data", count);
        } catch (Exception e) {
            log.error("查询数据库配置总数失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询数据库配置
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<DatabaseConfig> page = new Page<>(current, size);
            IPage<DatabaseConfig> pageResult = databaseConfigService.page(page);
            
            // 转换为DTO对象
            List<DatabaseConfigListDTO> dtoList = pageResult.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
            
            // 构建分页结果
            IPage<DatabaseConfigListDTO> dtoPageResult = new Page<>(current, size, pageResult.getTotal());
            dtoPageResult.setRecords(dtoList);
            
            result.put("success", true);
            result.put("data", dtoPageResult);
        } catch (Exception e) {
            log.error("查询数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private DatabaseConfigListDTO convertToListDTO(DatabaseConfig config) {
        DatabaseConfigListDTO dto = new DatabaseConfigListDTO();
        dto.setId(config.getId());
        dto.setConfigName(config.getConfigName());
        
        // 解析JDBC URL获取主机、端口、数据库名称，同时设置脱敏URL
        dto.parseJdbcUrl(config.getJdbcUrl());
        
        dto.setUsername(config.getUsername());
        dto.setDriverClassName(config.getDriverClassName());
        dto.setEnabled(config.getEnabled());
        dto.setRemark(config.getRemark());
        dto.setCreateTime(config.getCreateTime());
        dto.setUpdateTime(config.getUpdateTime());
        return dto;
    }

    /**
     * 分页查询数据库配置
     */
    @GetMapping("/page")
    public Map<String, Object> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<DatabaseConfig> page = new Page<>(current, size);
            IPage<DatabaseConfig> pageResult = databaseConfigService.page(page);
            
            result.put("success", true);
            result.put("data", pageResult);
        } catch (Exception e) {
            log.error("查询数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有启用的数据库配置
     */
    @GetMapping("/enabled")
    public Map<String, Object> getEnabledConfigs() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DatabaseConfig> configs = databaseConfigService.getEnabledConfigs();
            result.put("success", true);
            result.put("data", configs);
        } catch (Exception e) {
            log.error("查询启用的数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询数据库配置
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConfig config = databaseConfigService.getById(id);
            result.put("success", true);
            result.put("data", config);
        } catch (Exception e) {
            log.error("查询数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存数据库配置
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody DatabaseConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查配置名称是否重复
            DatabaseConfig existing = databaseConfigService.getByConfigName(config.getConfigName());
            if (existing != null && !existing.getId().equals(config.getId())) {
                result.put("success", false);
                result.put("message", "配置名称已存在");
                return result;
            }
            
            boolean success = databaseConfigService.save(config);
            result.put("success", success);
            result.put("message", success ? "保存成功" : "保存失败");
        } catch (Exception e) {
            log.error("保存数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新数据库配置
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody DatabaseConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查配置名称是否重复
            DatabaseConfig existing = databaseConfigService.getByConfigName(config.getConfigName());
            if (existing != null && !existing.getId().equals(config.getId())) {
                result.put("success", false);
                result.put("message", "配置名称已存在");
                return result;
            }
            
            boolean success = databaseConfigService.updateById(config);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            log.error("更新数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新数据库配置（PUT方法）
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateById(@PathVariable Long id, @RequestBody DatabaseConfig config) {
        Map<String, Object> result = new HashMap<>();
        try {
            config.setId(id);
            // 检查配置名称是否重复
            DatabaseConfig existing = databaseConfigService.getByConfigName(config.getConfigName());
            if (existing != null && !existing.getId().equals(config.getId())) {
                result.put("success", false);
                result.put("message", "配置名称已存在");
                return result;
            }
            
            boolean success = databaseConfigService.updateById(config);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            log.error("更新数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除数据库配置
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = databaseConfigService.deleteConfig(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除数据库配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test")
    public Map<String, Object> testConnection(@RequestBody Map<String, Object> testData) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 从前端数据构建DatabaseConfig对象
            DatabaseConfig config = new DatabaseConfig();
            String databaseType = (String) testData.get("databaseType");
            String host = (String) testData.get("host");
            Integer port = (Integer) testData.get("port");
            String databaseSchema = (String) testData.get("databaseSchema");
            String username = (String) testData.get("username");
            String password = (String) testData.get("password");
            
            // 构建JDBC URL
            String jdbcUrl = "";
            String driverClassName = "";
            if ("mysql".equals(databaseType)) {
                jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=30000&socketTimeout=60000&autoReconnect=true&failOverReadOnly=false&maxReconnects=3", host, port, databaseSchema);
                driverClassName = "com.mysql.cj.jdbc.Driver";
            }
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);
            
            boolean success = databaseConfigService.testConnection(config);
            result.put("success", success);
            result.put("message", success ? "连接测试成功" : "连接测试失败");
        } catch (Exception e) {
            log.error("测试数据库连接失败", e);
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试已保存的数据库连接
     */
    @PostMapping("/{id}/test")
    public Map<String, Object> testConnectionById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConfig config = databaseConfigService.getById(id);
            if (config == null) {
                result.put("success", false);
                result.put("message", "数据库配置不存在");
                return result;
            }
            
            boolean success = databaseConfigService.testConnection(config);
            result.put("success", success);
            result.put("message", success ? "连接测试成功" : "连接测试失败");
        } catch (Exception e) {
            log.error("测试数据库连接失败", e);
            result.put("success", false);
            result.put("message", "连接测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 验证SQL语句
     */
    @PostMapping("/validate-sql")
    public Map<String, Object> validateSql(@RequestParam Long configId, @RequestParam String sql) {
        Map<String, Object> result = new HashMap<>();
        try {
            DatabaseConfig config = databaseConfigService.getById(configId);
            if (config == null) {
                result.put("success", false);
                result.put("message", "数据库配置不存在");
                return result;
            }
            
            Map<String, Object> validateResult = databaseService.validateSql(config, sql);
            result.put("success", true);
            result.put("data", validateResult);
        } catch (Exception e) {
            log.error("验证SQL语句失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}