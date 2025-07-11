package com.dingtalk.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 执行记录列表响应DTO
 */
@Data
public class ExecuteRecordListDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 执行ID
     */
    private String executionId;
    
    /**
     * 查询任务ID
     */
    private Long queryTaskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 执行状态（SUCCESS/FAILURE/RUNNING）
     */
    private String executeStatus;
    
    /**
     * 查询结果数量
     */
    private Integer resultCount;
    
    /**
     * 钉钉推送状态（SUCCESS/FAILURE/SKIP）
     */
    private String pushStatus;
    
    /**
     * 执行耗时（毫秒）
     */
    private Long executeDuration;
    
    /**
     * 执行开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 执行结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 是否告警
     */
    private Boolean isAlert;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 获取执行状态（兼容方法）
     */
    public String getStatus() {
        return this.executeStatus;
    }
}