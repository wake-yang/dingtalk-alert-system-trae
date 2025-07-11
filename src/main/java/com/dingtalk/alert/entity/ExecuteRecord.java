package com.dingtalk.alert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 执行记录实体
 * 
 * @author system
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("execute_record")
public class ExecuteRecord extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 执行ID（雪花片算法生成的唯一ID）
     */
    @TableField("execution_id")
    private String executionId;

    /**
     * 查询任务ID
     */
    @TableField("task_id")
    private Long queryTaskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 执行状态（SUCCESS/FAILURE/RUNNING）
     */
    @TableField("status")
    private String executeStatus;

    /**
     * 查询结果数量
     */
    private Integer resultCount;

    /**
     * 查询结果内容（JSON格式）
     */
    private String resultContent;

    /**
     * 钉钉推送状态（SUCCESS/FAILURE/SKIP）
     */
    @TableField("dingtalk_status")
    private String pushStatus;

    /**
     * 钉钉推送响应
     */
    @TableField("dingtalk_response")
    private String pushResponse;

    /**
     * 执行耗时（毫秒）
     */
    private Long executeDuration;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * SQL查询语句
     */
    private String sqlQuery;

    /**
     * 执行日志
     */
    private String logs;

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
    @TableField("is_alert")
    private Boolean isAlert;

    /**
     * 获取执行状态
     * 
     * @return 执行状态
     */
    public String getStatus() {
        return this.executeStatus;
    }
}