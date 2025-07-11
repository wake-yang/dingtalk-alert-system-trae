package com.dingtalk.alert.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 钉钉配置列表响应DTO
 */
@Data
public class DingTalkConfigListDTO {
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 配置名称
     */
    private String name;
    
    /**
     * Webhook地址（脱敏显示）
     */
    private String webhookUrl;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 创建人
     */
    private String createBy;
    
    /**
     * 更新人
     */
    private String updateBy;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 设置脱敏的Webhook地址
     */
    public void setWebhookUrlMasked(String originalUrl) {
        if (originalUrl != null && originalUrl.length() > 30) {
            this.webhookUrl = originalUrl.substring(0, 30) + "***" + originalUrl.substring(originalUrl.length() - 10);
        } else {
            this.webhookUrl = originalUrl;
        }
    }
}