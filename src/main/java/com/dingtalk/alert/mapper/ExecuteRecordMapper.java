package com.dingtalk.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.alert.dto.DashboardRecordDTO;
import com.dingtalk.alert.entity.ExecuteRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 执行记录Mapper接口
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface ExecuteRecordMapper extends BaseMapper<ExecuteRecord> {

    /**
     * 分页查询执行记录
     * 
     * @param page 分页参数
     * @param queryTaskId 查询任务ID
     * @param executeStatus 执行状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行记录分页
     */
    IPage<ExecuteRecord> selectRecordPage(Page<ExecuteRecord> page, 
                                     @Param("queryTaskId") Long queryTaskId,
                                     @Param("executionId") String executionId, // 新增参数
                                     @Param("executeStatus") String executeStatus,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最近的执行记录（完整对象）
     * 
     * @param queryTaskId 查询任务ID
     * @param limit 限制数量
     * @return 执行记录列表
     */
    List<ExecuteRecord> selectRecentRecords(@Param("queryTaskId") Long queryTaskId, 
                                           @Param("limit") Integer limit);

    /**
     * 查询最近的执行记录（仅首页必要字段）
     * 
     * @param limit 限制数量
     * @return 执行记录列表
     */
    List<DashboardRecordDTO> selectRecentRecordsForDashboard(@Param("limit") Integer limit);

    /**
     * 统计执行记录
     * 
     * @param queryTaskId 查询任务ID
     * @param executeStatus 执行状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录数量
     */
    Long countRecords(@Param("queryTaskId") Long queryTaskId,
                     @Param("executeStatus") String executeStatus,
                     @Param("startTime") LocalDateTime startTime,
                     @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期统计执行记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 每日统计数据
     */
    List<Map<String, Object>> selectDailyStatistics(@Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 获取所有不同的任务ID
     * 
     * @return 任务ID列表
     */
    List<Long> selectDistinctTaskIds();
}