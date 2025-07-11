package com.dingtalk.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 查询任务列表响应DTO
 */
@Data
public class QueryTaskListDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 数据库配置名称
     */
    private String databaseConfigName;
    
    /**
     * 执行频率（cron表达式）
     */
    private String cronExpression;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 任务状态（RUNNING/STOPPED/ERROR）
     */
    private String status;
    
    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;
    
    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecuteTime;
    
    /**
     * 执行次数
     */
    private Integer executeCount;
    
    /**
     * 成功次数
     */
    private Integer successCount;
    
    /**
     * 失败次数
     */
    private Integer failureCount;
    
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
}