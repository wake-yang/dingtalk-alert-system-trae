package com.dingtalk.alert.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.dingtalk.alert.entity.DatabaseConfig;
import com.dingtalk.alert.service.DatabaseConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库连接服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DatabaseService {

    /**
     * 数据源缓存
     */
    private final Map<String, DruidDataSource> dataSourceCache = new ConcurrentHashMap<>();

    /**
     * 获取数据源
     * 
     * @param config 数据库配置
     * @return 数据源
     */
    public DruidDataSource getDataSource(DatabaseConfig config) {
        String key = generateCacheKey(config);
        
        return dataSourceCache.computeIfAbsent(key, k -> {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(config.getJdbcUrl());
            dataSource.setUsername(config.getUsername());
            dataSource.setPassword(config.getPassword());
            dataSource.setDriverClassName(config.getDriverClassName());

            // --- Start of changes ---

            // 增加JDBC连接属性，设置网络超时
            Properties connectProps = new Properties();
            // Socket读写超时时间，单位毫秒，设置为5分钟
            connectProps.setProperty("socketTimeout", "300000");
            // 连接超时时间，单位毫秒
            connectProps.setProperty("connectTimeout", "30000");
            dataSource.setConnectProperties(connectProps);
            
            // 连接池配置
            dataSource.setInitialSize(2);
            dataSource.setMinIdle(2);
            dataSource.setMaxActive(10);
            dataSource.setMaxWait(30000); // 连接等待超时30秒
            dataSource.setTimeBetweenEvictionRunsMillis(60000);
            dataSource.setMinEvictableIdleTimeMillis(300000);
            dataSource.setValidationQuery(config.getTestSql() != null ? config.getTestSql() : "SELECT 1");
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(true); // 获取连接时验证
            dataSource.setTestOnReturn(false);

            // 配置关闭长时间未使用的连接
            dataSource.setRemoveAbandoned(true);
            dataSource.setRemoveAbandonedTimeout(1800); // 超过30分钟未使用的连接被关闭
            dataSource.setLogAbandoned(true); // 将关闭的连接信息记入日志

            // --- End of changes ---

            dataSource.setConnectionErrorRetryAttempts(3); // 连接失败重试3次
            dataSource.setBreakAfterAcquireFailure(false); // 获取连接失败后不中断
            
            try {
                dataSource.init();
                log.info("数据源初始化成功: {}", config.getConfigName());
            } catch (Exception e) {
                log.error("数据源初始化失败: {}", config.getConfigName(), e);
                throw new RuntimeException("数据源初始化失败", e);
            }
            
            return dataSource;
        });
    }

    /**
     * 执行查询SQL
     * 
     * @param config 数据库配置
     * @param sql SQL语句
     * @return 查询结果
     */
    public List<Map<String, Object>> executeQuery(DatabaseConfig config, String sql) {
        DruidDataSource dataSource = getDataSource(config);
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
            
            log.info("SQL执行成功，返回{}条记录", results.size());
            
        } catch (Exception e) {
            log.error("SQL执行失败: {}", sql, e);
            throw new RuntimeException("SQL执行失败: " + e.getMessage(), e);
        }
        
        return results;
    }

    /**
     * 测试数据库连接
     * 
     * @param config 数据库配置
     * @return 测试结果
     */
    public boolean testConnection(DatabaseConfig config) {
        try {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(config.getJdbcUrl());
            dataSource.setUsername(config.getUsername());
            dataSource.setPassword(config.getPassword());
            dataSource.setDriverClassName(config.getDriverClassName());
            
            // 设置连接超时和重试参数
            dataSource.setMaxWait(30000); // 连接等待超时30秒
            dataSource.setConnectionErrorRetryAttempts(3); // 连接失败重试3次
            dataSource.setBreakAfterAcquireFailure(false); // 获取连接失败后不中断
            dataSource.setValidationQuery(config.getTestSql() != null ? config.getTestSql() : "SELECT 1");
            dataSource.setTestOnBorrow(true); // 获取连接时验证
            dataSource.setTestWhileIdle(false); // 测试时不验证空闲连接
            
            dataSource.init();
            
            try (Connection connection = dataSource.getConnection()) {
                String testSql = config.getTestSql() != null ? config.getTestSql() : "SELECT 1";
                try (PreparedStatement statement = connection.prepareStatement(testSql)) {
                    statement.executeQuery();
                }
            }
            
            dataSource.close();
            log.info("数据库连接测试成功: {}", config.getConfigName());
            return true;
            
        } catch (Exception e) {
            log.error("数据库连接测试失败: {}", config.getConfigName(), e);
            return false;
        }
    }

    /**
     * 验证SQL语句
     * 
     * @param config 数据库配置
     * @param sql SQL语句
     * @return 验证结果
     */
    public Map<String, Object> validateSql(DatabaseConfig config, String sql) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 添加LIMIT限制，避免查询大量数据
            String testSql = sql;
            if (!sql.toLowerCase().contains("limit")) {
                testSql = sql + " LIMIT 1";
            }
            
            List<Map<String, Object>> data = executeQuery(config, testSql);
            
            result.put("valid", true);
            result.put("message", "SQL语句验证成功");
            result.put("columnCount", data.isEmpty() ? 0 : data.get(0).size());
            result.put("columns", data.isEmpty() ? new ArrayList<>() : new ArrayList<>(data.get(0).keySet()));
            result.put("sampleData", data);
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "SQL语句验证失败: " + e.getMessage());
            result.put("columnCount", 0);
            result.put("columns", new ArrayList<>());
            result.put("sampleData", new ArrayList<>());
        }
        
        return result;
    }


    /**
     * 生成缓存键
     * 
     * @param config 数据库配置
     * @return 缓存键
     */
    private String generateCacheKey(DatabaseConfig config) {
        return config.getId() + "_" + config.getJdbcUrl() + "_" + config.getUsername();
    }
}