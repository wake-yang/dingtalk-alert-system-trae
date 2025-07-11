package com.dingtalk.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dingtalk.alert.dto.DashboardRecordDTO;
import com.dingtalk.alert.entity.ExecuteRecord;
import com.dingtalk.alert.mapper.ExecuteRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行记录服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class ExecuteRecordService extends ServiceImpl<ExecuteRecordMapper, ExecuteRecord> {

    /**
     * 保存执行记录
     * 
     * @param record 执行记录
     * @return 保存结果
     */
    @Override
    @Transactional
    public boolean save(ExecuteRecord record) {
        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }
        return super.save(record);
    }


    /**
     * 分页查询执行记录（重载方法）
     * 
     * @param page 分页对象
     * @param queryTaskId 查询任务ID
     * @param executeStatus 执行状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行记录分页
     */
    public IPage<ExecuteRecord> getRecordsByPage(Page<ExecuteRecord> page, Long queryTaskId, 
                                            String executionId, String executeStatus,
                                            String startTime, String endTime) {
        QueryWrapper<ExecuteRecord> wrapper = new QueryWrapper<>();

        if (queryTaskId != null) {
            wrapper.eq("task_id", queryTaskId);
        }
        if (executionId != null && !executionId.trim().isEmpty()) {
            wrapper.eq("execution_id", executionId);
        }
        if (executeStatus != null && !executeStatus.trim().isEmpty()) {
            wrapper.eq("status", executeStatus);
        }
        if (startTime != null && !startTime.trim().isEmpty()) {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime.replace(" ", "T"));
                wrapper.ge("create_time", start);
            } catch (Exception e) {
                log.warn("时间格式解析失败: {}", startTime);
            }
        }
        if (endTime != null && !endTime.trim().isEmpty()) {
            try {
                LocalDateTime end = LocalDateTime.parse(endTime.replace(" ", "T"));
                wrapper.le("create_time", end);
            } catch (Exception e) {
                log.warn("时间格式解析失败: {}", endTime);
            }
        }

        wrapper.orderByDesc("create_time");

        return page(page, wrapper);
    }

    /**
     * 查询最近的执行记录
     * 
     * @param queryTaskId 查询任务ID
     * @param limit 限制数量
     * @return 执行记录列表
     */
    public List<ExecuteRecord> getRecentRecords(Long queryTaskId, Integer limit) {
        // 由于 selectRecentRecordsForDashboard 返回的是 DashboardRecordDTO，
        // 这里需要使用其他方法或者修改返回类型
        // 暂时注释掉，需要重新实现
        // return baseMapper.selectRecentRecords(queryTaskId, limit != null ? limit : 10);
        throw new UnsupportedOperationException("Method needs to be reimplemented");
    }

    /**
     * 查询最近的执行记录（重载方法）
     * 
     * @param limit 限制数量
     * @return 执行记录列表
     */
    public List<ExecuteRecord> getRecentRecords(int limit) {
        // 同样需要重新实现
        // return baseMapper.selectRecentRecords(null, limit);
        throw new UnsupportedOperationException("Method needs to be reimplemented");
    }

    /**
     * 获取首页仪表板的最近执行记录
     * 
     * @param limit 限制数量
     * @return 轻量级执行记录列表
     */
    public List<DashboardRecordDTO> getRecentRecordsForDashboard(int limit) {
        return baseMapper.selectRecentRecordsForDashboard(limit);
    }

    /**
     * 统计执行记录
     * 
     * @param queryTaskId 查询任务ID
     * @param executeStatus 执行状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录数量
     */
    public Long countRecords(Long queryTaskId, String executeStatus, 
                            LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.countRecords(queryTaskId, executeStatus, startTime, endTime);
    }

    /**
     * 获取执行统计信息
     * 
     * @param taskId 任务ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计信息
     */
    public java.util.Map<String, Object> getExecuteStatistics(Long taskId, String startTime, String endTime) {
        java.util.Map<String, Object> statistics = new java.util.HashMap<>();
        
        try {
            LocalDateTime start = null;
            LocalDateTime end = null;
            
            // 解析时间参数
            if (startTime != null && !startTime.trim().isEmpty()) {
                start = LocalDateTime.parse(startTime.replace(" ", "T"));
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                end = LocalDateTime.parse(endTime.replace(" ", "T"));
            }
            
            // 总执行次数
            Long totalCount = countRecords(taskId, null, start, end);
            statistics.put("totalCount", totalCount);
            
            // 成功次数
            Long successCount = countRecords(taskId, "SUCCESS", start, end);
            statistics.put("successCount", successCount);
            
            // 失败次数
            Long failureCount = countRecords(taskId, "FAILURE", start, end);
            statistics.put("failureCount", failureCount);
            
            // 成功率
            double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
            statistics.put("successRate", Math.round(successRate * 100.0) / 100.0);
            
          /*  // 最近执行记录
            List<ExecuteRecord> recentRecords = getRecentRecords(taskId, 10);
            statistics.put("recentRecords", recentRecords);*/
            
        } catch (Exception e) {
            log.error("获取执行统计信息失败", e);
            statistics.put("error", e.getMessage());
        }
        
        return statistics;
    }


    /**
     * 获取所有任务的执行统计信息
     * 
     * @return 任务执行统计信息列表
     */
    public List<java.util.Map<String, Object>> getTaskExecuteStatistics() {
        List<java.util.Map<String, Object>> statisticsList = new java.util.ArrayList<>();
        
        try {
            // 获取所有不同的任务ID
            List<Long> taskIds = baseMapper.selectDistinctTaskIds();
            
            for (Long taskId : taskIds) {
                java.util.Map<String, Object> taskStats = new java.util.HashMap<>();
                taskStats.put("taskId", taskId);
                
                // 总执行次数
                Long totalCount = countRecords(taskId, null, null, null);
                taskStats.put("totalCount", totalCount);
                
                // 成功次数
                Long successCount = countRecords(taskId, "SUCCESS", null, null);
                taskStats.put("successCount", successCount);
                
                // 失败次数
                Long failureCount = countRecords(taskId, "FAILURE", null, null);
                taskStats.put("failureCount", failureCount);
                
                // 成功率
                double successRate = totalCount > 0 ? (double) successCount / totalCount * 100 : 0;
                taskStats.put("successRate", Math.round(successRate * 100.0) / 100.0);
                
                // 最近执行时间
                List<ExecuteRecord> recentRecords = getRecentRecords(taskId, 1);
                if (!recentRecords.isEmpty()) {
                    taskStats.put("lastExecuteTime", recentRecords.get(0).getCreateTime());
                }
                
                statisticsList.add(taskStats);
            }
            
        } catch (Exception e) {
            log.error("获取任务执行统计信息失败", e);
        }
        
        return statisticsList;
    }

    /**
     * 获取导出记录
     * 
     * @param taskId 任务ID
     * @param status 执行状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行记录列表
     */
    public List<ExecuteRecord> getRecordsForExport(Long taskId, String status, String startTime, String endTime) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExecuteRecord> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            
            if (taskId != null) {
                wrapper.eq("task_id", taskId);
            }
            if (status != null && !status.trim().isEmpty()) {
                wrapper.eq("status", status);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                wrapper.ge("create_time", startTime);
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                wrapper.le("create_time", endTime);
            }
            
            wrapper.orderByDesc("create_time");
            
            return list(wrapper);
            
        } catch (Exception e) {
            log.error("获取导出记录失败", e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 清理过期的执行记录
     * 
     * @param days 保留天数
     * @return 清理数量
     */
    @Transactional
    public int cleanupExpiredRecords(int days) {
        return cleanExpiredRecords(days);
    }

    /**
     * 清理过期的执行记录
     * 
     * @param days 保留天数
     * @return 清理数量
     */
    @Transactional
    public int cleanExpiredRecords(int days) {
        try {
            LocalDateTime expireTime = LocalDateTime.now().minusDays(days);
            
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ExecuteRecord> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            wrapper.lt("create_time", expireTime);
            
            int count = (int) count(wrapper);
            remove(wrapper);
            
            log.info("清理过期执行记录完成，清理数量: {}", count);
            return count;
            
        } catch (Exception e) {
            log.error("清理过期执行记录失败", e);
            return 0;
        }
    }

    /**
     * 获取每日执行统计
     * 
     * @param days 统计天数
     * @return 每日统计数据
     */
    public List<Map<String, Object>> getDailyStatistics(int days) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(days - 1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        return baseMapper.selectDailyStatistics(startTime, endTime);
    }

    /**
     * 获取仪表板数据
     * @return 仪表板数据
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> dashboardData = new HashMap<>();
        
        try {
            // 执行记录总数
            long recordCount = this.count();
            dashboardData.put("recordCount", recordCount);
            
            // 最近执行记录（限制5条，仅必要字段）
            List<DashboardRecordDTO> recentRecords = this.getRecentRecordsForDashboard(5);
            dashboardData.put("recentRecords", recentRecords);
            
            // 每日统计数据（最近7天）
            List<Map<String, Object>> dailyStats = this.getDailyStatistics(7);
            dashboardData.put("dailyStatistics", dailyStats);
            
            return dashboardData;
            
        } catch (Exception e) {
            log.error("获取仪表板数据失败", e);
            throw new RuntimeException("获取仪表板数据失败: " + e.getMessage());
        }
    }
}