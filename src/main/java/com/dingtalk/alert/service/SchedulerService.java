package com.dingtalk.alert.service;

import com.dingtalk.alert.entity.QueryTask;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

/**
 * 定时任务调度服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class SchedulerService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private QueryTaskService queryTaskService;

    /**
     * 初始化调度器
     */
    @PostConstruct
    public void init() {
        try {
            // 启动调度器
            if (!scheduler.isStarted()) {
                scheduler.start();
                log.info("定时任务调度器启动成功");
            }
            
            // 加载所有启用的任务
            loadAllEnabledTasks();
            
        } catch (Exception e) {
            log.error("初始化定时任务调度器失败", e);
        }
    }

    /**
     * 销毁调度器
     */
    @PreDestroy
    public void destroy() {
        try {
            if (scheduler != null && scheduler.isStarted()) {
                scheduler.shutdown(true);
                log.info("定时任务调度器关闭成功");
            }
        } catch (Exception e) {
            log.error("关闭定时任务调度器失败", e);
        }
    }

    /**
     * 加载所有启用的任务
     */
    public void loadAllEnabledTasks() {
        try {
            List<QueryTask> enabledTasks = queryTaskService.getEnabledTasks();
            for (QueryTask task : enabledTasks) {
                scheduleTask(task);
            }
            log.info("加载启用任务完成，共{}个任务", enabledTasks.size());
        } catch (Exception e) {
            log.error("加载启用任务失败", e);
        }
    }

    /**
     * 调度任务
     * 
     * @param task 查询任务
     */
    public void scheduleTask(QueryTask task) {
        try {
            String jobName = "QueryTask_" + task.getId();
            String groupName = "QueryTaskGroup";
            
            // 删除已存在的任务
            unscheduleTask(task.getId());
            
            // 创建JobDetail
            JobDetail jobDetail = JobBuilder.newJob(QueryTaskJob.class)
                    .withIdentity(jobName, groupName)
                    .usingJobData("taskId", task.getId())
                    .build();
            
            // 创建Trigger
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobName + "_Trigger", groupName)
                    .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                    .build();
            
            // 调度任务
            scheduler.scheduleJob(jobDetail, trigger);
            
            log.info("任务调度成功: {} - {}", task.getTaskName(), task.getCronExpression());
            
        } catch (Exception e) {
            log.error("调度任务失败: {}", task.getTaskName(), e);
        }
    }

    /**
     * 取消调度任务
     * 
     * @param taskId 任务ID
     */
    public void unscheduleTask(Long taskId) {
        try {
            String jobName = "QueryTask_" + taskId;
            String groupName = "QueryTaskGroup";
            
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("取消任务调度成功: {}", taskId);
            }
            
        } catch (Exception e) {
            log.error("取消任务调度失败: {}", taskId, e);
        }
    }

    /**
     * 暂停任务
     * 
     * @param taskId 任务ID
     */
    public void pauseTask(Long taskId) {
        try {
            String jobName = "QueryTask_" + taskId;
            String groupName = "QueryTaskGroup";
            
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            if (scheduler.checkExists(jobKey)) {
                scheduler.pauseJob(jobKey);
                log.info("暂停任务成功: {}", taskId);
            }
            
        } catch (Exception e) {
            log.error("暂停任务失败: {}", taskId, e);
        }
    }


    /**
     * 移除任务
     * 
     * @param taskId 任务ID
     */
    public void removeTask(Long taskId) {
        try {
            String jobName = "QueryTask_" + taskId;
            String groupName = "QueryTaskGroup";
            
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("移除任务成功: {}", taskId);
            } else {
                log.warn("任务不存在，无法移除: {}", taskId);
            }
            
        } catch (Exception e) {
            log.error("移除任务失败: {}", taskId, e);
        }
    }

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    public String getTaskStatus(Long taskId) {
        try {
            String jobName = "QueryTask_" + taskId;
            String groupName = "QueryTaskGroup";
            
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            if (scheduler.checkExists(jobKey)) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (!triggers.isEmpty()) {
                    Trigger.TriggerState state = scheduler.getTriggerState(triggers.get(0).getKey());
                    return state.name();
                }
            }
            return "NOT_SCHEDULED";
            
        } catch (Exception e) {
            log.error("获取任务状态失败: {}", taskId, e);
            return "ERROR";
        }
    }

    /**
     * 验证Cron表达式
     * 
     * @param cronExpression Cron表达式
     * @return 验证结果
     */
    public boolean isValidCronExpression(String cronExpression) {
        try {
            CronScheduleBuilder.cronSchedule(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取下次执行时间
     * 
     * @param cronExpression Cron表达式
     * @return 下次执行时间
     */
    public java.time.LocalDateTime getNextExecuteTime(String cronExpression) {
        try {
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();
            
            java.util.Date nextFireTime = trigger.getFireTimeAfter(new java.util.Date());
            if (nextFireTime != null) {
                return java.time.LocalDateTime.ofInstant(
                    nextFireTime.toInstant(), 
                    java.time.ZoneId.systemDefault()
                );
            }
            return null;
            
        } catch (Exception e) {
            log.error("计算下次执行时间失败: {}", cronExpression, e);
            return null;
        }
    }

    /**
     * Quartz任务执行类
     */
    public static class QueryTaskJob implements Job {
        
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                JobDataMap dataMap = context.getJobDetail().getJobDataMap();
                Long taskId = dataMap.getLong("taskId");
                
                // 从Spring容器获取服务
                QueryTaskService queryTaskService = null;
                
                // 首先尝试通过ApplicationContextProvider获取
                try {
                    queryTaskService = com.dingtalk.alert.config.ApplicationContextProvider.getBean(QueryTaskService.class);
                } catch (Exception e) {
                    log.warn("无法通过ApplicationContextProvider获取QueryTaskService，尝试其他方式", e);
                }
                
                // 如果ApplicationContextProvider获取失败，尝试通过WebApplicationContext获取
                if (queryTaskService == null) {
                    try {
                        org.springframework.web.context.WebApplicationContext webApplicationContext = 
                            org.springframework.web.context.ContextLoader.getCurrentWebApplicationContext();
                        if (webApplicationContext != null) {
                            queryTaskService = webApplicationContext.getBean(QueryTaskService.class);
                        }
                    } catch (Exception e) {
                        log.warn("无法通过ContextLoader获取QueryTaskService", e);
                    }
                }
                
                if (queryTaskService == null) {
                    throw new JobExecutionException("无法获取QueryTaskService实例，任务执行失败");
                }
                
                // 执行任务
                queryTaskService.executeTask(taskId);
                
            } catch (Exception e) {
                log.error("执行定时任务失败", e);
                throw new JobExecutionException(e);
            }
        }
    }
}