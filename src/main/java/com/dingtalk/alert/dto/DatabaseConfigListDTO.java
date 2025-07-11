package com.dingtalk.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据库配置列表响应DTO
 */
@Data
public class DatabaseConfigListDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 数据库连接URL（脱敏显示）
     */
    private String jdbcUrl;
    
    /**
     * 数据库主机
     */
    private String host;
    
    /**
     * 数据库端口
     */
    private String port;
    
    /**
     * 数据库名称
     */
    private String databaseName;
    
    /**
     * 数据库用户名
     */
    private String username;
    
    /**
     * 数据库驱动类名
     */
    private String driverClassName;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 设置脱敏的数据库URL
     */
    public void setJdbcUrlMasked(String originalUrl) {
        if (originalUrl != null && originalUrl.length() > 30) {
            int atIndex = originalUrl.indexOf("@");
            if (atIndex > 0) {
                this.jdbcUrl = originalUrl.substring(0, atIndex) + "@***" + originalUrl.substring(originalUrl.lastIndexOf("/"));
            } else {
                this.jdbcUrl = originalUrl.substring(0, 20) + "***";
            }
        } else {
            this.jdbcUrl = originalUrl;
        }
    }
    
    /**
     * 从JDBC URL解析并设置主机、端口、数据库名称
     */
    public void parseJdbcUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isEmpty()) {
            return;
        }
        
        try {
            // 设置脱敏URL
            setJdbcUrlMasked(originalUrl);
            
            // 解析主机、端口、数据库名称
            // 支持常见的JDBC URL格式：
            // jdbc:mysql://host:port/database
            // jdbc:postgresql://host:port/database
            // jdbc:oracle:thin:@host:port:database
            // jdbc:sqlserver://host:port;databaseName=database
            
            if (originalUrl.contains("mysql") || originalUrl.contains("postgresql")) {
                // MySQL/PostgreSQL格式: jdbc:mysql://host:port/database
                String[] parts = originalUrl.split("://");
                if (parts.length > 1) {
                    String hostPart = parts[1];
                    String[] hostAndDb = hostPart.split("/");
                    if (hostAndDb.length > 0) {
                        String[] hostAndPort = hostAndDb[0].split(":");
                        this.host = hostAndPort[0];
                        if (hostAndPort.length > 1) {
                            // 移除可能的参数
                            String portStr = hostAndPort[1].split("\\?")[0];
                            this.port = portStr;
                        }
                    }
                    if (hostAndDb.length > 1) {
                        // 移除可能的参数
                        String dbName = hostAndDb[1].split("\\?")[0];
                        this.databaseName = dbName;
                    }
                }
            } else if (originalUrl.contains("oracle")) {
                // Oracle格式: jdbc:oracle:thin:@host:port:database
                String[] parts = originalUrl.split("@");
                if (parts.length > 1) {
                    String hostPart = parts[1];
                    String[] hostPortDb = hostPart.split(":");
                    if (hostPortDb.length >= 3) {
                        this.host = hostPortDb[0];
                        this.port = hostPortDb[1];
                        this.databaseName = hostPortDb[2];
                    }
                }
            } else if (originalUrl.contains("sqlserver")) {
                // SQL Server格式: jdbc:sqlserver://host:port;databaseName=database
                String[] parts = originalUrl.split("://");
                if (parts.length > 1) {
                    String hostPart = parts[1];
                    String[] hostAndParams = hostPart.split(";");
                    if (hostAndParams.length > 0) {
                        String[] hostAndPort = hostAndParams[0].split(":");
                        this.host = hostAndPort[0];
                        if (hostAndPort.length > 1) {
                            this.port = hostAndPort[1];
                        }
                    }
                    // 查找databaseName参数
                    for (String param : hostAndParams) {
                        if (param.startsWith("databaseName=")) {
                            this.databaseName = param.substring("databaseName=".length());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 解析失败时，至少设置脱敏URL
            setJdbcUrlMasked(originalUrl);
        }
    }
}