package com.dingtalk.alert.controller;

import com.dingtalk.alert.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 首页仪表板控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DatabaseConfigService databaseConfigService;
    
    @Autowired
    private AlertTemplateService alertTemplateService;
    
    @Autowired
    private QueryTaskService queryTaskService;
    
    @Autowired
    private ExecuteRecordService executeRecordService;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 获取首页所有数据
     */
    @GetMapping("/overview")
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 并行获取各种统计数据
            CompletableFuture<Long> dbCountFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return databaseConfigService.count();
                } catch (Exception e) {
                    log.error("获取数据库配置数量失败", e);
                    return 0L;
                }
            }, executor);
            
            CompletableFuture<Long> templateCountFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return alertTemplateService.count();
                } catch (Exception e) {
                    log.error("获取告警模板数量失败", e);
                    return 0L;
                }
            }, executor);
            
            CompletableFuture<Long> taskCountFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return queryTaskService.count();
                } catch (Exception e) {
                    log.error("获取查询任务数量失败", e);
                    return 0L;
                }
            }, executor);
            
            CompletableFuture<Map<String, Object>> recordDataFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeRecordService.getDashboardData();
                } catch (Exception e) {
                    log.error("获取执行记录数据失败", e);
                    return new HashMap<>();
                }
            }, executor);
            
            // 添加系统状态信息
            CompletableFuture<Map<String, Object>> systemInfoFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return getSystemInfo();
                } catch (Exception e) {
                    log.error("获取系统状态失败", e);
                    return new HashMap<>();
                }
            }, executor);
            
            // 等待所有异步任务完成
            CompletableFuture.allOf(dbCountFuture, templateCountFuture, taskCountFuture, recordDataFuture, systemInfoFuture).join();
            
            // 组装结果
            result.put("success", true);
            result.put("dbConfigCount", dbCountFuture.get());
            result.put("templateCount", templateCountFuture.get());
            result.put("taskCount", taskCountFuture.get());
            result.put("recordData", recordDataFuture.get());
            result.put("systemInfo", systemInfoFuture.get());
            
        } catch (Exception e) {
            log.error("获取首页数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
        }
        
        return result;
    }
    
    private Map<String, Object> getSystemInfo() {
        Map<String, Object> systemInfo = new HashMap<>();
        
        try {
            // 获取JVM运行时信息
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            
            // 启动时间
            long startTime = runtimeBean.getStartTime();
            systemInfo.put("startTime", new Date(startTime));
            
            // 运行时间（毫秒）
            long uptime = runtimeBean.getUptime();
            systemInfo.put("uptime", uptime);
            
            // JVM内存使用情况
            MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
            long usedMemory = heapMemory.getUsed() / 1024 / 1024; // MB
            long maxMemory = heapMemory.getMax() / 1024 / 1024; // MB
            systemInfo.put("jvmMemory", usedMemory + "MB / " + maxMemory + "MB");
            
            // 活跃线程数
            int activeThreads = threadBean.getThreadCount();
            systemInfo.put("activeThreads", activeThreads);
            
            // 系统CPU使用率（简化版本）
            systemInfo.put("systemCpu", "N/A");
            
            // 数据库连接数（需要根据实际连接池实现）
            systemInfo.put("dbConnections", "N/A");
            
        } catch (Exception e) {
            log.error("获取系统信息失败", e);
        }
        
        return systemInfo;
    }
}