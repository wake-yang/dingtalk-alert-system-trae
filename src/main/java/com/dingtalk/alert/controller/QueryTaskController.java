package com.dingtalk.alert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.alert.entity.AlertTemplate;
import com.dingtalk.alert.entity.DatabaseConfig;
import com.dingtalk.alert.entity.QueryTask;
import com.dingtalk.alert.service.AlertTemplateService;
import com.dingtalk.alert.service.DatabaseConfigService;
import com.dingtalk.alert.service.QueryTaskService;
import com.dingtalk.alert.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 查询任务控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/task")
public class QueryTaskController {

    @Autowired
    private QueryTaskService queryTaskService;

    @Autowired
    private SchedulerService schedulerService;
    
    @Autowired
    private AlertTemplateService alertTemplateService;

    @Autowired
    private DatabaseConfigService databaseConfigService;
    
    // 简单的内存缓存，避免频繁查询
    private volatile Long cachedCount = null;
    private volatile long lastCountCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存

    /**
     * 获取查询任务总数
     */
    @GetMapping("/count")
    public Map<String, Object> count() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 使用缓存优化性能
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCountCacheTime < CACHE_DURATION && cachedCount != null) {
                result.put("success", true);
                result.put("data", cachedCount);
                return result;
            }
            
            long count = queryTaskService.count();
            cachedCount = count;
            lastCountCacheTime = currentTime;
            
            result.put("success", true);
            result.put("data", count);
        } catch (Exception e) {
            log.error("查询查询任务总数失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询查询任务
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "10") long size) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<QueryTask> page = new Page<>(current, size);
            IPage<QueryTask> pageResult = queryTaskService.page(page);
            
            // 添加任务状态信息和数据库配置名称
            pageResult.getRecords().forEach(task -> {
                String status = schedulerService.getTaskStatus(task.getId());
                task.setStatus(status);
                
                // 获取数据库配置名称
                if (task.getDatabaseConfigId() != null) {
                    DatabaseConfig dbConfig = databaseConfigService.getById(task.getDatabaseConfigId());
                    if (dbConfig != null) {
                        // 添加一个临时字段存储数据库配置名称
                        task.setDatabaseConfigName(dbConfig.getConfigName());
                    }
                }
            });
            
            result.put("success", true);
            result.put("data", pageResult);
        } catch (Exception e) {
            log.error("查询查询任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有启用的查询任务
     */
    @GetMapping("/enabled")
    public Map<String, Object> getEnabledTasks() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<QueryTask> tasks = queryTaskService.getEnabledTasks();
            result.put("success", true);
            result.put("data", tasks);
        } catch (Exception e) {
            log.error("查询启用的查询任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询查询任务
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            QueryTask task = queryTaskService.getById(id);
            if (task != null) {
                String status = schedulerService.getTaskStatus(task.getId());
                task.setStatus(status);
                
                // 如果是自定义模板，从告警模板表中获取自定义模板信息
                if ("custom".equals(task.getTemplateMode()) && task.getAlertTemplateId() != null) {
                    AlertTemplate customTemplate = alertTemplateService.getById(task.getAlertTemplateId());
                    if (customTemplate != null && customTemplate.getIsCustom()) {
                        Map<String, Object> customTemplateData = new HashMap<>();
                        customTemplateData.put("templateName", customTemplate.getTemplateName());
                        customTemplateData.put("templateType", customTemplate.getTemplateType());
                        customTemplateData.put("templateContent", customTemplate.getTemplateContent());
                        task.setCustomTemplateData(customTemplateData);
                    }
                }
            }
            result.put("success", true);
            result.put("data", task);
        } catch (Exception e) {
            log.error("查询查询任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存查询任务
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody QueryTask task) {
        return createTask(task);
    }

    /**
     * 创建查询任务 (RESTful POST方法)
     */
    @PostMapping
    public Map<String, Object> createTask(@RequestBody QueryTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证任务对象不为空
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务对象不能为空");
                return result;
            }
            
            // 验证Cron表达式
            if (task.getCronExpression() == null || !schedulerService.isValidCronExpression(task.getCronExpression())) {
                result.put("success", false);
                result.put("message", "Cron表达式格式错误");
                return result;
            }
            
            // 验证任务配置
            Map<String, Object> validateResult = queryTaskService.validateTask(task);
            if (!(Boolean) validateResult.get("valid")) {
                result.put("success", false);
                result.put("message", validateResult.get("message"));
                return result;
            }
            
            // 处理自定义模板
            if ("custom".equals(task.getTemplateMode()) && task.getCustomTemplateData() != null && !task.getCustomTemplateData().isEmpty()) {
                // 创建自定义告警模板
                AlertTemplate customTemplate = new AlertTemplate();
                customTemplate.setTemplateName((String) task.getCustomTemplateData().get("templateName"));
                customTemplate.setTemplateType((String) task.getCustomTemplateData().get("templateType"));
                customTemplate.setTemplateContent((String) task.getCustomTemplateData().get("templateContent"));
                customTemplate.setEnabled(true);
                customTemplate.setIsCustom(true);
                customTemplate.setRemark("任务自定义模板");
                
                // 保存自定义模板
                boolean templateSaved = alertTemplateService.save(customTemplate);
                if (!templateSaved) {
                    result.put("success", false);
                    result.put("message", "保存自定义模板失败");
                    return result;
                }
                
                // 设置任务关联的模板ID
                task.setAlertTemplateId(customTemplate.getId());
                
                // 更新模板的关联任务ID
                customTemplate.setTaskId(task.getId());
                alertTemplateService.updateById(customTemplate);
            }
            
            // 在创建任务时设置一致的状态
            // 设置初始值
            task.setExecuteCount(0);
            task.setSuccessCount(0);
            task.setFailureCount(0);
            task.setStatus("stopped");  // 改为小写，与前端保持一致
            task.setEnabled(false);     // 新建任务默认未启用
            
            boolean success = queryTaskService.save(task);
            
            // 如果是自定义模板，需要在任务保存后更新模板的关联任务ID
            if (success && "custom".equals(task.getTemplateMode()) && task.getAlertTemplateId() != null) {
                AlertTemplate template = alertTemplateService.getById(task.getAlertTemplateId());
                if (template != null && template.getIsCustom()) {
                    template.setTaskId(task.getId());
                    alertTemplateService.updateById(template);
                }
            }
            if (success && task.getEnabled() != null && task.getEnabled()) {
                // 调度任务
                schedulerService.scheduleTask(task);
            }
            
            result.put("success", success);
            result.put("message", success ? "保存成功" : "保存失败");
        } catch (Exception e) {
            log.error("保存查询任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新查询任务
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody QueryTask task) {
        return updateTask(task);
    }

    /**
     * 更新查询任务 (RESTful PUT方法)
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateTask(@PathVariable Long id, @RequestBody QueryTask task) {
        task.setId(id); // 确保ID正确设置
        return updateTask(task);
    }

    /**
     * 更新查询任务的通用方法
     */
    private Map<String, Object> updateTask(QueryTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证任务对象不为空
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务对象不能为空");
                return result;
            }
            
            // 验证任务ID不为空
            if (task.getId() == null) {
                result.put("success", false);
                result.put("message", "任务ID不能为空");
                return result;
            }
            
            // 验证Cron表达式
            if (task.getCronExpression() == null || !schedulerService.isValidCronExpression(task.getCronExpression())) {
                result.put("success", false);
                result.put("message", "Cron表达式格式错误");
                return result;
            }
            
            // 验证任务配置
            Map<String, Object> validateResult = queryTaskService.validateTask(task);
            if (!(Boolean) validateResult.get("valid")) {
                result.put("success", false);
                result.put("message", validateResult.get("message"));
                return result;
            }
            
            // 获取原任务信息
            QueryTask originalTask = queryTaskService.getById(task.getId());
            if (originalTask == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            // 处理自定义模板
            if ("custom".equals(task.getTemplateMode()) && task.getCustomTemplateData() != null && !task.getCustomTemplateData().isEmpty()) {
                // 检查是否已有自定义模板
                AlertTemplate existingTemplate = null;
                if (originalTask.getAlertTemplateId() != null) {
                    existingTemplate = alertTemplateService.getById(originalTask.getAlertTemplateId());
                }
                
                if (existingTemplate != null && existingTemplate.getIsCustom() && task.getId().equals(existingTemplate.getTaskId())) {
                    // 更新现有自定义模板
                    existingTemplate.setTemplateName((String) task.getCustomTemplateData().get("templateName"));
                    existingTemplate.setTemplateType((String) task.getCustomTemplateData().get("templateType"));
                    existingTemplate.setTemplateContent((String) task.getCustomTemplateData().get("templateContent"));
                    alertTemplateService.updateById(existingTemplate);
                    task.setAlertTemplateId(existingTemplate.getId());
                } else {
                    // 创建新的自定义模板
                    AlertTemplate customTemplate = new AlertTemplate();
                    customTemplate.setTemplateName((String) task.getCustomTemplateData().get("templateName"));
                    customTemplate.setTemplateType((String) task.getCustomTemplateData().get("templateType"));
                    customTemplate.setTemplateContent((String) task.getCustomTemplateData().get("templateContent"));
                    customTemplate.setEnabled(true);
                    customTemplate.setIsCustom(true);
                    customTemplate.setTaskId(task.getId());
                    customTemplate.setRemark("任务自定义模板");
                    
                    boolean templateSaved = alertTemplateService.save(customTemplate);
                    if (!templateSaved) {
                        result.put("success", false);
                        result.put("message", "保存自定义模板失败");
                        return result;
                    }
                    
                    task.setAlertTemplateId(customTemplate.getId());
                }
            } else if ("existing".equals(task.getTemplateMode())) {
                // 如果从自定义模板切换到现有模板，删除原自定义模板
                if (originalTask.getAlertTemplateId() != null) {
                    AlertTemplate originalTemplate = alertTemplateService.getById(originalTask.getAlertTemplateId());
                    if (originalTemplate != null && originalTemplate.getIsCustom() && task.getId().equals(originalTemplate.getTaskId())) {
                        alertTemplateService.removeById(originalTemplate.getId());
                    }
                }
            }
            
            // 处理钉钉配置
            // 处理钉钉配置
            if ("custom".equals(task.getDingtalkMode())) {
                // 使用自定义配置
                if (task.getCustomDingtalkData() != null && !task.getCustomDingtalkData().isEmpty()) {
                    task.setDingtalkConfigId(null); // 清除已选的配置ID
                } else {
                    result.put("success", false);
                    result.put("message", "自定义钉钉配置数据不能为空");
                    return result;
                }
            } else if ("existing".equals(task.getDingtalkMode())) {
                // 使用现有配置
                if (task.getDingtalkConfigId() == null) {
                    result.put("success", false);
                    result.put("message", "请选择钉钉配置");
                    return result;
                }
                task.setCustomDingtalkData(null); // 清除自定义配置数据
            } else {
                // 如果没有指定模式，默认清除所有钉钉配置
                task.setDingtalkConfigId(null);
                task.setCustomDingtalkData(null);
            }

            boolean success = queryTaskService.updateById(task);
            if (success) {
                // 重新调度任务
                schedulerService.unscheduleTask(task.getId());
                if (task.getEnabled() != null && task.getEnabled()) {
                    schedulerService.scheduleTask(task);
                }
            }
            
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            log.error("更新查询任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除查询任务
     */
    @PostMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        return deleteTask(id);
    }

    /**
     * 删除查询任务 (RESTful DELETE方法)
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 取消调度
            schedulerService.unscheduleTask(id);
            
            // 逻辑删除
            QueryTask task = queryTaskService.getById(id);
            if (task != null) {
                task.setDeleted(true);
                task.setEnabled(false);
                boolean success = queryTaskService.removeById(task);
                result.put("success", success);
                result.put("message", success ? "删除成功" : "删除失败");
            } else {
                result.put("success", false);
                result.put("message", "任务不存在");
            }
        } catch (Exception e) {
            log.error("删除查询任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 启用/禁用任务
     */
    @PostMapping("/toggle/{id}")
    public Map<String, Object> toggleTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            QueryTask task = queryTaskService.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            boolean newEnabled = task.getEnabled() == null ? true : !task.getEnabled();
            task.setEnabled(newEnabled);
            
            boolean success = queryTaskService.updateById(task);
            if (success) {
                if (newEnabled) {
                    schedulerService.scheduleTask(task);
                } else {
                    schedulerService.unscheduleTask(id);
                }
            }
            
            result.put("success", success);
            result.put("message", success ? (newEnabled ? "启用成功" : "禁用成功") : "操作失败");
        } catch (Exception e) {
            log.error("切换任务状态失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 立即执行任务
     */
    @PostMapping("/execute/{id}")
    public Map<String, Object> executeTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = queryTaskService.executeTask(id);
            result.put("success", success);
            result.put("message", success ? "执行成功" : "执行失败");
        } catch (Exception e) {
            log.error("执行任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 立即执行任务 - RESTful风格
     */
    @PostMapping("/{id}/execute")
    public Map<String, Object> executeTaskRestful(@PathVariable Long id) {
        return executeTask(id);
    }

    /**
     * 启动任务
     */
    @PostMapping("/{id}/start")
    public Map<String, Object> startTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            QueryTask task = queryTaskService.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            // 启用任务并设置状态为运行中
            task.setEnabled(true);
            task.setStatus("running");
            
            // 计算下次执行时间
            try {
                CronExpression cronExpression = new CronExpression(task.getCronExpression());
                Date nextTime = cronExpression.getNextValidTimeAfter(new Date());
                if (nextTime != null) {
                    task.setNextExecuteTime(LocalDateTime.ofInstant(nextTime.toInstant(), ZoneId.systemDefault()));
                }
            } catch (Exception e) {
                log.warn("计算下次执行时间失败: {}", e.getMessage());
            }
            
            boolean success = queryTaskService.updateById(task);
            if (success) {
                schedulerService.scheduleTask(task);
            }
            
            result.put("success", success);
            result.put("message", success ? "任务启动成功" : "任务启动失败");
        } catch (Exception e) {
            log.error("启动任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 暂停任务
     */
    @PostMapping("/{id}/pause")
    public Map<String, Object> pauseTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查任务是否存在
            QueryTask task = queryTaskService.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            // 暂停调度器中的任务
            schedulerService.pauseTask(id);
            
            // 更新任务状态为暂停
            task.setStatus("paused");
            queryTaskService.updateById(task);
            
            result.put("success", true);
            result.put("message", "任务暂停成功");
        } catch (Exception e) {
            log.error("暂停任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public Map<String, Object> stopTask(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查任务是否存在
            QueryTask task = queryTaskService.getById(id);
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务不存在");
                return result;
            }
            
            // 从调度器中移除任务
            schedulerService.removeTask(id);
            
            // 更新任务状态为停止，并清空下次执行时间
            task.setStatus("stopped");
            task.setEnabled(false);
            task.setNextExecuteTime(null); // 清空下次执行时间
            queryTaskService.updateById(task);
            
            result.put("success", true);
            result.put("message", "任务停止成功");
        } catch (Exception e) {
            log.error("停止任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 验证任务配置并测试SQL（会连接数据库）
     */
    @PostMapping("/validate")
    public Map<String, Object> validateTask(@RequestBody QueryTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证任务对象不为空
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务对象不能为空");
                return result;
            }
            
            Map<String, Object> validateResult = queryTaskService.validateTaskWithSql(task);
            result.put("success", true);
            result.put("data", validateResult);
        } catch (Exception e) {
            log.error("验证任务配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 测试发送 - 验证任务配置并发送钉钉消息（复用executeTask核心逻辑）
     */
    @PostMapping("/test-send")
    public Map<String, Object> testSend(@RequestBody QueryTask task) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证任务对象不为空
            if (task == null) {
                result.put("success", false);
                result.put("message", "任务对象不能为空");
                return result;
            }
            
            // 先验证任务配置
            Map<String, Object> validateResult = queryTaskService.validateTaskWithSql(task);
            if (!(Boolean) validateResult.get("valid")) {
                result.put("success", false);
                result.put("message", "任务配置验证失败：" + validateResult.get("message"));
                return result;
            }
            
            // 使用重构后的testSendDingTalk方法，复用executeTask的核心逻辑
            boolean sendResult = queryTaskService.testSendDingTalk(task);
            if (sendResult) {
                result.put("success", true);
                result.put("message", "测试发送成功，请检查钉钉群消息。测试使用了与正式执行相同的业务逻辑。");
            } else {
                result.put("success", false);
                result.put("message", "测试发送失败");
            }
        } catch (Exception e) {
            log.error("测试发送失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取Cron表达式示例
     */
    @GetMapping("/cron-examples")
    public Map<String, Object> getCronExamples() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> examples = java.util.Arrays.asList(
            createMap("expression", "0 */5 * * * ?", "description", "每5分钟执行一次"),
            createMap("expression", "0 0 */1 * * ?", "description", "每小时执行一次"),
            createMap("expression", "0 0 9 * * ?", "description", "每天上午9点执行"),
            createMap("expression", "0 0 9 * * MON-FRI", "description", "工作日上午9点执行"),
            createMap("expression", "0 0 0 1 * ?", "description", "每月1号凌晨执行")
        );
            result.put("success", true);
            result.put("data", examples);
        } catch (Exception e) {
            log.error("获取Cron表达式示例失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 创建Map的辅助方法
     */
    private Map<String, String> createMap(String key1, String value1, String key2, String value2) {
        Map<String, String> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }
}