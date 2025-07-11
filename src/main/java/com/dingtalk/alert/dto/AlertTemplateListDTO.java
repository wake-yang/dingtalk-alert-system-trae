package com.dingtalk.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警模板列表响应DTO
 */
@Data
public class AlertTemplateListDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板类型
     */
    private String templateType;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 是否自定义模板
     */
    private Boolean isCustom;
    
    /**
     * 关联任务ID（仅自定义模板有值）
     */
    private Long taskId;
    
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