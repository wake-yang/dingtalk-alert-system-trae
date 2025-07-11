package com.dingtalk.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 首页执行记录DTO
 */
@Data
public class DashboardRecordDTO {
    /**
     * 查询任务ID
     */
    private Long queryTaskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 执行状态（SUCCESS/FAILURE）
     */
    private String executeStatus;
    
    /**
     * 创建时间（执行时间）
     */
    private LocalDateTime createTime;
    
    // 为了兼容前端，添加别名方法
    public LocalDateTime getExecuteTime() {
        return this.createTime;
    }
    
    public String getStatus() {
        return this.executeStatus;
    }
}