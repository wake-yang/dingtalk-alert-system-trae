package com.dingtalk.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 自定义SQL查询任务实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("query_task")
public class QueryTask extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 自定义SQL语句
     */
    private String sqlContent;

    /**
     * 数据库配置ID
     */
    private Long databaseConfigId;

    /**
     * 告警模板ID
     */
    private Long alertTemplateId;

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
     * 告警条件
     */
    private String alertCondition;

    /**
     * 告警阈值
     */
    private Integer threshold;

    /**
     * SQL结果模式：single_row、multi_row
     */
    private String resultMode;

    /**
     * 多行模式目标字段名
     */
    private String targetField;

    /**
     * 多行模式结果格式：table、list、json
     */
    private String resultFormat;

    /**
     * 阈值模式：count、value
     */
    private String thresholdMode;

    /**
     * 阈值字段名
     */
    private String thresholdField;

    /**
     * 钉钉配置ID
     */
    private Long dingtalkConfigId;

    /**
     * 是否@所有人
     */
    private Boolean atAll;

    /**
     * @指定人员手机号
     */
    private String atMobiles;

    /**
     * 自定义钉钉配置数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private java.util.Map<String, Object> customDingtalkData;

    /**
     * 钉钉配置模式: existing-现有配置, custom-自定义配置
     */
    private String dingtalkMode;

    /**
     * 模板模式：existing-现有模板，custom-自定义模板
     */
    private String templateMode;

    /**
     * 自定义模板配置数据
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private java.util.Map<String, Object> customTemplateData;

    /**
     * 数据库配置名称（非数据库字段，仅用于前端显示）
     */
    @TableField(exist = false)
    private String databaseConfigName;
}