package com.dingtalk.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 钉钉告警模板实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("alert_template")
public class AlertTemplate extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String templateName;
    private String templateContent;
    private String templateFields;
    private String templateType;
    
    private Boolean enabled;
    private Boolean isCustom;
    private Long taskId;
    private String remark;
}