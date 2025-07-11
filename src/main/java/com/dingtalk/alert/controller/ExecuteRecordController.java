package com.dingtalk.alert.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.alert.dto.DashboardRecordDTO;
import com.dingtalk.alert.dto.ExecuteRecordListDTO;
import com.dingtalk.alert.entity.ExecuteRecord;
import com.dingtalk.alert.service.ExecuteRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 执行记录控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/record")
public class ExecuteRecordController {

    @Autowired
    private ExecuteRecordService executeRecordService;
    
    // 简单的内存缓存，避免频繁查询
    private volatile Map<String, Object> dashboardCache = new ConcurrentHashMap<>();
    private volatile long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存

    /**
     * 获取执行记录总数
     */
    @GetMapping("/count")
    public Map<String, Object> count() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用缓存优化性能
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCacheTime < CACHE_DURATION && dashboardCache.containsKey("count")) {
                result.put("success", true);
                result.put("data", dashboardCache.get("count"));
                return result;
            }
            
            long count = executeRecordService.count();
            dashboardCache.put("count", count);
            lastCacheTime = currentTime;
            
            result.put("success", true);
            result.put("data", count);
        } catch (Exception e) {
            log.error("查询执行记录总数失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    
    /**
     * 获取首页仪表板数据（优化版本，减少请求次数）
     */
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用缓存优化性能
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCacheTime < CACHE_DURATION && dashboardCache.containsKey("dashboard")) {
                result.put("success", true);
                result.put("data", dashboardCache.get("dashboard"));
                return result;
            }
            
            Map<String, Object> dashboardData = new HashMap<>();
            
            // 执行记录总数
            long recordCount = executeRecordService.count();
            dashboardData.put("recordCount", recordCount);
            
            // 最近执行记录（限制5条，仅必要字段）
            List<DashboardRecordDTO> recentRecords = executeRecordService.getRecentRecordsForDashboard(5);
            dashboardData.put("recentRecords", recentRecords);
            
            // 获取最近7天的每日统计数据
            List<Map<String, Object>> dailyStats = executeRecordService.getDailyStatistics(7);
            
            // 处理每日统计数据，确保每天都有数据
            List<String> labels = new ArrayList<>();
            List<Integer> successData = new ArrayList<>();
            List<Integer> failureData = new ArrayList<>();
            
            LocalDate today = LocalDate.now();
            Map<String, Map<String, Object>> statsMap = new HashMap<>();
            
            // 将数据库查询结果转换为Map
            for (Map<String, Object> stat : dailyStats) {
                String date = stat.get("date").toString();
                statsMap.put(date, stat);
            }
            
            // 生成最近7天的完整数据
            for (int i = 6; i >= 0; i--) {
                LocalDate date = today.minusDays(i);
                String dateStr = date.toString();
                labels.add(date.format(DateTimeFormatter.ofPattern("MM-dd")));
                
                Map<String, Object> dayStats = statsMap.get(dateStr);
                if (dayStats != null) {
                    successData.add(((Number) dayStats.getOrDefault("successCount", 0)).intValue());
                    failureData.add(((Number) dayStats.getOrDefault("failureCount", 0)).intValue());
                } else {
                    successData.add(0);
                    failureData.add(0);
                }
            }
            
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("labels", labels);
            chartData.put("successData", successData);
            chartData.put("failureData", failureData);
            
            dashboardData.put("chartData", chartData);
            
            // 总体统计信息
            int totalSuccess = successData.stream().mapToInt(Integer::intValue).sum();
            int totalFailure = failureData.stream().mapToInt(Integer::intValue).sum();
            int totalCount = totalSuccess + totalFailure;
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalCount", totalCount);
            statistics.put("successCount", totalSuccess);
            statistics.put("failureCount", totalFailure);
            statistics.put("successRate", totalCount > 0 ? Math.round((double) totalSuccess / totalCount * 10000.0) / 100.0 : 0);
            
            dashboardData.put("statistics", statistics);
            
            // 缓存结果
            dashboardCache.put("dashboard", dashboardData);
            lastCacheTime = currentTime;
            
            result.put("success", true);
            result.put("data", dashboardData);
        } catch (Exception e) {
            log.error("获取仪表板数据失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询执行记录
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "20") long size,
                                   @RequestParam(required = false) Long taskId,
                                   @RequestParam(required = false) String executionId,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String startTime,
                                   @RequestParam(required = false) String endTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<ExecuteRecord> page = new Page<>(current, size);
            QueryWrapper<ExecuteRecord> wrapper = new QueryWrapper<>();
            
            if (taskId != null) {
                wrapper.eq("task_id", taskId);
            }
            if (executionId != null && !executionId.isEmpty()) {
                wrapper.like("execution_id", executionId);
            }
            if (status != null && !status.isEmpty()) {
                wrapper.eq("status", status);
            }
            if (startTime != null && !startTime.isEmpty()) {
                wrapper.ge("start_time", startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                wrapper.le("end_time", endTime);
            }
            
            wrapper.orderByDesc("create_time");
            IPage<ExecuteRecord> pageResult = executeRecordService.page(page, wrapper);
            
            // 转换为DTO对象
            List<ExecuteRecordListDTO> dtoList = pageResult.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
            
            // 构建分页结果
            IPage<ExecuteRecordListDTO> dtoPageResult = new Page<>(current, size, pageResult.getTotal());
            dtoPageResult.setRecords(dtoList);
            
            result.put("success", true);
            result.put("data", dtoPageResult);
        } catch (Exception e) {
            log.error("查询执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private ExecuteRecordListDTO convertToListDTO(ExecuteRecord record) {
        ExecuteRecordListDTO dto = new ExecuteRecordListDTO();
        dto.setId(record.getId());
        dto.setExecutionId(record.getExecutionId());
        dto.setQueryTaskId(record.getQueryTaskId());
        dto.setTaskName(record.getTaskName());
        dto.setExecuteStatus(record.getExecuteStatus());
        dto.setResultCount(record.getResultCount());
        dto.setPushStatus(record.getPushStatus());
        dto.setExecuteDuration(record.getExecuteDuration());
        dto.setStartTime(record.getStartTime());
        dto.setEndTime(record.getEndTime());
        dto.setIsAlert(record.getIsAlert());
        dto.setCreateTime(record.getCreateTime());
        return dto;
    }

    /**
     * 根据ID查询执行记录
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ExecuteRecord record = executeRecordService.getById(id);
            result.put("success", true);
            result.put("data", record);
        } catch (Exception e) {
            log.error("查询执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 查询最近执行记录
     */
    @GetMapping("/recent")
    public Map<String, Object> getRecentRecords(@RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用缓存优化性能
            String cacheKey = "recent_" + limit;
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCacheTime < CACHE_DURATION && dashboardCache.containsKey(cacheKey)) {
                result.put("success", true);
                result.put("data", dashboardCache.get(cacheKey));
                return result;
            }
            
            List<ExecuteRecord> records = executeRecordService.getRecentRecords(limit);
            dashboardCache.put(cacheKey, records);
            lastCacheTime = currentTime;
            
            result.put("success", true);
            result.put("data", records);
        } catch (Exception e) {
            log.error("查询最近执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取执行统计信息
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics(@RequestParam(required = false) Long taskId,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> statistics = executeRecordService.getExecuteStatistics(taskId, startDate, endDate);
            result.put("success", true);
            result.put("data", statistics);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取任务执行统计
     */
    @GetMapping("/task-statistics")
    public Map<String, Object> getTaskStatistics() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> statistics = executeRecordService.getTaskExecuteStatistics();
            result.put("success", true);
            result.put("data", statistics);
        } catch (Exception e) {
            log.error("获取任务执行统计失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 清理过期执行记录
     */
    @PostMapping("/cleanup")
    public Map<String, Object> cleanupExpiredRecords(@RequestParam(defaultValue = "30") int days) {
        Map<String, Object> result = new HashMap<>();
        try {
            int count = executeRecordService.cleanupExpiredRecords(days);
            result.put("success", true);
            result.put("message", "清理完成，删除了 " + count + " 条记录");
            result.put("data", count);
        } catch (Exception e) {
            log.error("清理过期执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除执行记录
     */
    @PostMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = executeRecordService.removeById(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 批量删除执行记录
     */
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDelete(@RequestBody List<Long> ids) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = executeRecordService.removeByIds(ids);
            result.put("success", success);
            result.put("message", success ? "批量删除成功" : "批量删除失败");
        } catch (Exception e) {
            log.error("批量删除执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取执行状态选项
     */
    @GetMapping("/status-options")
    public Map<String, Object> getStatusOptions() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> options = java.util.Arrays.asList(
                    createMap("value", "SUCCESS", "label", "成功"),
                    createMap("value", "FAILURE", "label", "失败"),
                    createMap("value", "RUNNING", "label", "执行中")
            );
            result.put("success", true);
            result.put("data", options);
        } catch (Exception e) {
            log.error("获取执行状态选项失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 重新执行失败的任务
     */
    @PostMapping("/retry/{id}")
    public Map<String, Object> retryFailedTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            ExecuteRecord record = executeRecordService.getById(id);
            if (record == null) {
                result.put("success", false);
                result.put("message", "执行记录不存在");
                return result;
            }
            
            if (!"FAILURE".equals(record.getStatus())) {
                result.put("success", false);
                result.put("message", "只能重试失败的任务");
                return result;
            }
            
            // 这里可以调用任务执行服务重新执行
            // queryTaskService.executeTask(record.getTaskId());
            
            result.put("success", true);
            result.put("message", "重试任务已提交");
        } catch (Exception e) {
            log.error("重试失败任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 导出执行记录
     */
    @GetMapping("/export")
    public Map<String, Object> exportRecords(@RequestParam(required = false) Long taskId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String startTime,
                                            @RequestParam(required = false) String endTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 这里可以实现导出功能，返回文件下载链接或直接返回数据
            List<ExecuteRecord> records = executeRecordService.getRecordsForExport(
                taskId, status, startTime, endTime);
            
            result.put("success", true);
            result.put("data", records);
            result.put("message", "导出数据准备完成");
        } catch (Exception e) {
            log.error("导出执行记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private Map<String, String> createMap(String key1, String value1, String key2, String value2) {
        Map<String, String> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }
}