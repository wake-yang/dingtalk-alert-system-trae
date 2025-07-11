package com.dingtalk.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dingtalk.alert.entity.QueryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 查询任务Mapper接口
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface QueryTaskMapper extends BaseMapper<QueryTask> {

    /**
     * 查询所有启用的查询任务
     * 
     * @return 查询任务列表
     */
    @Select("SELECT * FROM query_task WHERE enabled = 1 AND deleted = 0 ORDER BY create_time DESC")
    List<QueryTask> selectEnabledTasks();

    /**
     * 根据任务名称查询
     * 
     * @param taskName 任务名称
     * @return 查询任务
     */
    @Select("SELECT * FROM query_task WHERE task_name = #{taskName} AND deleted = 0")
    QueryTask selectByTaskName(String taskName);

    /**
     * 更新任务执行信息
     * 
     * @param id 任务ID
     * @param status 任务状态
     * @param lastExecuteTime 最后执行时间
     * @param nextExecuteTime 下次执行时间
     * @param executeCount 执行次数
     * @param successCount 成功次数
     * @param failureCount 失败次数
     */
    @Update("UPDATE query_task SET status = #{status}, last_execute_time = #{lastExecuteTime}, " +
            "next_execute_time = #{nextExecuteTime}, execute_count = #{executeCount}, " +
            "success_count = #{successCount}, failure_count = #{failureCount}, " +
            "update_time = NOW() WHERE id = #{id}")
    void updateExecuteInfo(Long id, String status, LocalDateTime lastExecuteTime, 
                          LocalDateTime nextExecuteTime, Integer executeCount, 
                          Integer successCount, Integer failureCount);
}