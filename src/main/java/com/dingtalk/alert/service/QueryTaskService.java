package com.dingtalk.alert.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dingtalk.alert.config.ApplicationContextProvider;
import com.dingtalk.alert.entity.AlertTemplate;
import com.dingtalk.alert.entity.DatabaseConfig;
import com.dingtalk.alert.entity.DingTalkConfig;
import com.dingtalk.alert.entity.ExecuteRecord;
import com.dingtalk.alert.entity.QueryTask;
import com.dingtalk.alert.mapper.QueryTaskMapper;
import com.dingtalk.alert.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

/**
 * 查询任务服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class QueryTaskService extends ServiceImpl<QueryTaskMapper, QueryTask> {

    @Autowired
    private DatabaseConfigService databaseConfigService;

    @Autowired
    private AlertTemplateService alertTemplateService;

    @Autowired
    private ExecuteRecordService executeRecordService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DingTalkService dingTalkService;

    // 在类的开头添加雪花片ID生成器注入
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;


    /**
     * 执行查询任务
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    @Transactional
    public boolean executeTask(Long taskId) {
        QueryTask task = null;
        ExecuteRecord record = null;
        StringBuilder logs = new StringBuilder();
        boolean isAlert = false; // 添加告警标志

        // 获取任务信息
        task = getById(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }

        // 生成执行ID
        String executionId = "HG"+snowflakeIdGenerator.nextId();

        // 创建执行记录
        record = new ExecuteRecord();
        record.setExecutionId(executionId);  // 设置雪花片ID
        record.setQueryTaskId(task.getId());
        record.setTaskName(task.getTaskName());
        record.setExecuteStatus("RUNNING");
        record.setSqlQuery(task.getSqlContent());
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());


        logs.append("任务开始执行，执行ID: ").append(executionId).append("\n");
        logs.append("任务ID: ").append(task.getId()).append("\n");
        logs.append("任务名称: ").append(task.getTaskName()).append("\n");

        boolean success = false;
        String errorMessage = null;
        long sqlExecuteDuration = 0;

        try {
            logs.append("开始执行任务: ").append(task.getTaskName()).append("\n");

            // 获取数据库配置
            DatabaseConfig databaseConfig = databaseConfigService.getById(task.getDatabaseConfigId());
            if (databaseConfig == null) {
                throw new RuntimeException("数据库配置不存在");
            }

            logs.append("数据库配置: ").append(databaseConfig.getConfigName()).append("\n");

            // 记录SQL执行开始时间
            long sqlStartTime = System.currentTimeMillis();

            // 执行SQL查询
            List<Map<String, Object>> queryResults = databaseService.executeQuery(databaseConfig, task.getSqlContent());

            // 计算SQL执行时间
            sqlExecuteDuration = System.currentTimeMillis() - sqlStartTime;

            record.setResultCount(queryResults.size());
            record.setResultContent(JSON.toJSONString(queryResults));

            logs.append("SQL执行完成，耗时: ").append(sqlExecuteDuration).append("ms\n");
            logs.append("查询结果数量: ").append(queryResults.size()).append(" 条\n");

            // 修改后的告警逻辑：检查是否配置了告警模板
            AlertTemplate template = null;
            if (task.getAlertTemplateId() != null) {
                template = alertTemplateService.getById(task.getAlertTemplateId());
            }

            if (template != null) {
                // 如果配置了告警模板，则进行告警判断
                boolean shouldAlert = false;

                if (task.getAlertCondition() == null || task.getAlertCondition().isEmpty()) {
                    // 没有设置告警条件，直接发送
                    shouldAlert = true;
                    isAlert = true;
                    logs.append("未设置告警条件，直接发送告警\n");
                } else {
                    // 有告警条件，进行判断
                    shouldAlert = shouldTriggerAlert(task, queryResults);
                    if (shouldAlert) {
                        isAlert = true;
                        logs.append("告警条件满足，准备发送告警\n");
                    } else {
                        logs.append("告警条件不满足，不发送告警\n");
                    }
                }

                if (shouldAlert) {
                    try {
                        // 发送告警消息
                        String alertResult = sendDingTalkMessage(template, queryResults, task, sqlExecuteDuration,executionId);
                        logs.append("告警发送结果: ").append(alertResult).append("\n");
                    } catch (Exception e) {
                        logs.append("告警发送失败: ").append(e.getMessage()).append("\n");
                        log.error("发送告警失败", e);
                    }
                }
            } else {
                logs.append("未配置告警模板，跳过告警发送\n");
            }

            // 记录执行成功
            success = true;
            logs.append("任务执行成功\n");

        } catch (Exception e) {
            isAlert = false;
            success = false;
            errorMessage = e.getMessage();
            logs.append("任务执行失败: ").append(errorMessage).append("\n");
            log.error("执行任务失败: {}", taskId, e);
        } finally {
            // 设置执行记录
            record.setEndTime(LocalDateTime.now());
            record.setExecuteStatus(success ? "SUCCESS" : "FAILED");
            record.setErrorMessage(errorMessage);
            record.setLogs(logs.toString());
            record.setExecuteDuration(sqlExecuteDuration);  // 修改这里，使用正确的方法名
            record.setIsAlert(isAlert);

            // 保存执行记录
            try {
                executeRecordService.save(record);
            } catch (Exception e) {
                log.error("保存执行记录失败", e);
            }

            // 更新任务统计信息
            try {
                updateTaskStatistics(task, success);
            } catch (Exception e) {
                log.error("更新任务统计失败", e);
            }

            // 更新下次执行时间
            try {
                updateNextExecuteTime(task);
            } catch (Exception e) {
                log.error("更新下次执行时间失败", e);
            }
        }

        return success;
    }

    /**
     * 发送钉钉消息
     * 
     * @param template 告警模板
     * @param queryResults 查询结果
     * @param task 查询任务（用于获取钉钉配置）
     * @return 推送结果
     */
    // 修改sendDingTalkMessage方法签名，接收SQL执行耗时参数
    private String sendDingTalkMessage(AlertTemplate template, List<Map<String, Object>> queryResults, QueryTask task, long sqlExecuteDuration, String executionId) {
        try {
            // 处理@信息
            List<String> atMobiles = null;
            if (task.getAtMobiles() != null && !task.getAtMobiles().trim().isEmpty()) {
                atMobiles = Arrays.asList(task.getAtMobiles().split(","));
            }
    
            // 处理结果模式
            List<Map<String, Object>> processedResults = processQueryResults(queryResults, task);
            
            // 添加系统变量到所有结果中
            for (Map<String, Object> result : processedResults) {
                // 在成功执行的情况下
                addSystemVariables(result, task, queryResults.size(), sqlExecuteDuration, true, executionId);
            }
    
            // 根据模板类型发送消息
            String templateType = template.getTemplateType();
            
            if ("multi_row".equals(task.getResultMode())) {
                // 多行结果模式：只发送一条消息，使用处理后的数据
                if (!processedResults.isEmpty()) {
                    Map<String, Object> data = processedResults.get(0);
                    
                    if ("text".equals(templateType)) {
                        String content = template.getTemplateContent();
                        content = replaceTemplateVariables(content, data);
                        return sendDingTalkMessageByTask(task, content, atMobiles);
                        
                    } else if ("markdown".equals(templateType)) {
                        String content = template.getTemplateContent();
                        content = replaceTemplateVariables(content, data);
                        return sendDingTalkMarkdownByTask(task, "查询结果通知", content, atMobiles);
                        
                    } else {
                        // 自定义模板
                        return sendDingTalkTemplateByTask(task, template.getTemplateContent(), data, atMobiles);
                    }
                }
            } else {
                // 单行结果模式：原有逻辑
                if ("text".equals(templateType)) {
                    // 文本消息
                    StringBuilder content = new StringBuilder();
                    for (Map<String, Object> row : processedResults) {
                        String rowContent = template.getTemplateContent();
                        rowContent = replaceTemplateVariables(rowContent, row);
                        content.append(rowContent).append("\n");
                    }
                    
                    return sendDingTalkMessageByTask(task, content.toString(), atMobiles);
                    
                } else if ("markdown".equals(templateType)) {
                    // Markdown消息
                    StringBuilder content = new StringBuilder();
                    for (Map<String, Object> row : processedResults) {
                        String rowContent = template.getTemplateContent();
                        rowContent = replaceTemplateVariables(rowContent, row);
                        content.append(rowContent).append("\n");
                    }
                    
                    return sendDingTalkMarkdownByTask(task, "查询结果通知", content.toString(), atMobiles);
                    
                } else {
                    // 自定义模板 - 发送多条消息
                    StringBuilder results = new StringBuilder();
                    for (Map<String, Object> row : processedResults) {
                        String result = sendDingTalkTemplateByTask(task, template.getTemplateContent(), row, atMobiles);
                        results.append(result).append(";");
                    }
                    return results.toString();
                }
            }
            
        } catch (Exception e) {
            log.error("发送钉钉消息失败", e);
            // 添加系统变量到错误信息中
            Map<String, Object> errorData = new HashMap<>();
            addSystemVariables(errorData, task, 0, sqlExecuteDuration, false,executionId);
            return "发送失败: " + e.getMessage();
        }
        
        return "发送成功";
    }

    /**
     * 添加系统变量
     */
    private void addSystemVariables(Map<String, Object> data, QueryTask task, int totalRows, long executeDuration, boolean success, String executionId) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        // 通知时间
        data.put("notify_time", now.format(formatter));
        
        // 执行耗时
        data.put("execute_duration", executeDuration + "ms");
        
        // 执行结果
        data.put("execute_result", success ? "成功" : "失败");
        
        // 任务名称
        data.put("task_name", task.getTaskName());
        
        // 结果行数
        data.put("total_rows", totalRows);
        
        // 当前时间（保持兼容性）
        data.put("current_time", now.format(formatter));
        
        // 执行ID（新增）
        data.put("execution_id", executionId != null ? executionId.toString() : "");
        
        // 任务ID
        data.put("task_id", task.getId());
    }

    /**
     * 替换模板变量
     */
    private String replaceTemplateVariables(String content, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            content = content.replace(placeholder, value);
        }
        return content;
    }

    /**
     * 根据QueryTask配置发送钉钉文本消息
     */
    private String sendDingTalkMessageByTask(QueryTask task, String content, List<String> atMobiles) {
        if (task.getDingtalkConfigId() != null) {
            // 使用现有钉钉配置
            DingTalkConfig dingTalkConfig = dingTalkService.getById(task.getDingtalkConfigId());
            if (dingTalkConfig == null) {
                throw new RuntimeException("钉钉配置不存在");
            }
            return dingTalkService.sendMessage(dingTalkConfig, content, task.getAtAll(), atMobiles);
        } else if (task.getCustomDingtalkData() != null) {
            // 使用自定义钉钉配置
            Map<String, Object> customData = task.getCustomDingtalkData();
            String webhookUrl = (String) customData.get("webhookUrl");
            String secret = (String) customData.get("secret");
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                throw new RuntimeException("钉钉Webhook URL不能为空");
            }
            
            return dingTalkService.sendTextMessage(
                webhookUrl,
                secret,
                content,
                task.getAtAll(),
                atMobiles
            );
        } else {
            throw new RuntimeException("未配置钉钉发送方式");
        }
    }

    /**
     * 根据QueryTask配置发送钉钉Markdown消息
     */
    private String sendDingTalkMarkdownByTask(QueryTask task, String title, String content, List<String> atMobiles) {
        if (task.getDingtalkConfigId() != null) {
            // 使用现有钉钉配置
            DingTalkConfig dingTalkConfig = dingTalkService.getById(task.getDingtalkConfigId());
            if (dingTalkConfig == null) {
                throw new RuntimeException("钉钉配置不存在");
            }
            return dingTalkService.sendMarkdownMessage(
                dingTalkConfig.getWebhookUrl(), 
                dingTalkConfig.getSecret(), 
                title, 
                content, 
                task.getAtAll(), 
                atMobiles
            );
        } else if (task.getCustomDingtalkData() != null) {
            // 使用自定义钉钉配置
            Map<String, Object> customData = task.getCustomDingtalkData();
            String webhookUrl = (String) customData.get("webhookUrl");
            String secret = (String) customData.get("secret");
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                throw new RuntimeException("钉钉Webhook URL不能为空");
            }
            
            return dingTalkService.sendMarkdownMessage(
                webhookUrl,
                secret,
                title,
                content,
                task.getAtAll(),
                atMobiles
            );
        } else {
            throw new RuntimeException("未配置钉钉发送方式");
        }
    }

    /**
     * 根据QueryTask配置发送钉钉模板消息
     */
    private String sendDingTalkTemplateByTask(QueryTask task, String templateContent, Map<String, Object> data, List<String> atMobiles) {
        if (task.getDingtalkConfigId() != null) {
            // 使用现有钉钉配置
            DingTalkConfig dingTalkConfig = dingTalkService.getById(task.getDingtalkConfigId());
            if (dingTalkConfig == null) {
                throw new RuntimeException("钉钉配置不存在");
            }
            return dingTalkService.sendTemplateMessage(
                dingTalkConfig.getWebhookUrl(),
                dingTalkConfig.getSecret(),
                templateContent,
                data,
                task.getAtAll(),
                atMobiles
            );
        } else if (task.getCustomDingtalkData() != null) {
            // 使用自定义钉钉配置
            Map<String, Object> customData = task.getCustomDingtalkData();
            String webhookUrl = (String) customData.get("webhookUrl");
            String secret = (String) customData.get("secret");
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                throw new RuntimeException("钉钉Webhook URL不能为空");
            }
            
            return dingTalkService.sendTemplateMessage(
                webhookUrl,
                secret,
                templateContent,
                data,
                task.getAtAll(),
                atMobiles
            );
        } else {
            throw new RuntimeException("未配置钉钉发送方式");
        }
    }

    /**
     * 处理查询结果根据结果模式
     * 
     * @param queryResults 原始查询结果
     * @param task 查询任务
     * @return 处理后的结果
     */
    private List<Map<String, Object>> processQueryResults(List<Map<String, Object>> queryResults, QueryTask task) {
        if (queryResults == null || queryResults.isEmpty()) {
            return queryResults;
        }
        
        // 如果是多行结果模式，需要将所有行数据合并到一个字段中
        if ("multi_row".equals(task.getResultMode())) {
            String targetField = task.getTargetField();
            String resultFormat = task.getResultFormat();
            
            if (targetField == null || targetField.trim().isEmpty()) {
                // 如果没有设置目标字段，根据结果格式自动设置
                switch (resultFormat) {
                    case "table":
                        targetField = "table_data";
                        break;
                    case "list":
                        targetField = "list_data";
                        break;
                    case "json":
                        targetField = "json_data";
                        break;
                    default:
                        targetField = "result_data";
                }
            }
            
            // 根据格式处理多行数据
            String formattedData = formatMultiRowData(queryResults, resultFormat);
            
            // 创建单行结果，包含格式化后的数据
            Map<String, Object> singleResult = new HashMap<>();
            singleResult.put(targetField, formattedData);
            
            // 添加统计信息
            singleResult.put("total_rows", queryResults.size());
            singleResult.put("current_time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return Arrays.asList(singleResult);
        }
        
        // 单行结果模式，直接返回查询结果
        return queryResults;
    }
    
    /**
     * 格式化多行数据
     */
    private String formatMultiRowData(List<Map<String, Object>> queryResults, String resultFormat) {
        try {
            switch (resultFormat) {
                case "table":
                    return formatAsTable(queryResults);
                case "list":
                    return formatAsList(queryResults);
                case "json":
                    return formatAsJson(queryResults);
                default:
                    return formatAsTable(queryResults);
            }
        } catch (Exception e) {
            log.error("格式化多行数据失败", e);
            return "数据格式化失败: " + e.getMessage();
        }
    }
    
    /**
     * 格式化为表格格式
     */
    private String formatAsTable(List<Map<String, Object>> queryResults) {
        if (queryResults.isEmpty()) {
            return "暂无数据";
        }
        
        StringBuilder table = new StringBuilder();
        
        // 获取表头
        Set<String> headers = queryResults.get(0).keySet();
        
        // 构建表格
        table.append("| ");
        for (String header : headers) {
            table.append(header).append(" | ");
        }
        table.append("\n");
        
        // 分隔线
        table.append("| ");
        for (String header : headers) {
            table.append("--- | ");
        }
        table.append("\n");
        
        // 数据行
        for (Map<String, Object> row : queryResults) {
            table.append("| ");
            for (String header : headers) {
                Object value = row.get(header);
                table.append(value != null ? value.toString() : "").append(" | ");
            }
            table.append("\n");
        }
        
        return table.toString();
    }
    
    /**
     * 格式化为列表格式
     */
    private String formatAsList(List<Map<String, Object>> queryResults) {
        if (queryResults.isEmpty()) {
            return "暂无数据";
        }
        
        StringBuilder list = new StringBuilder();
        
        for (int i = 0; i < queryResults.size(); i++) {
            Map<String, Object> row = queryResults.get(i);
            
            // 只有在多行结果时才显示记录序号
            if (queryResults.size() > 1) {
                list.append("**第").append(i + 1).append("条记录：**\n");
            }
            
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                list.append("- ").append(entry.getKey()).append(": ")
                    .append(entry.getValue() != null ? entry.getValue().toString() : "")
                    .append("\n");
            }
            list.append("\n");
        }
        
        return list.toString();
    }
    
    /**
     * 格式化为JSON格式
     */
    private String formatAsJson(List<Map<String, Object>> queryResults) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 注册JavaTimeModule以支持Java 8时间类型
            objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            // 禁用将日期写为时间戳的功能
            objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryResults);
        } catch (Exception e) {
            log.error("JSON格式化失败", e);
            return "JSON格式化失败: " + e.getMessage();
        }
    }
    


    /**
     * 更新任务的下次执行时间
     * 
     * @param task 查询任务
     */
    private void updateNextExecuteTime(QueryTask task) {
        try {
            // 获取SchedulerService实例
            SchedulerService schedulerService = ApplicationContextProvider.getBean(SchedulerService.class);
            
            // 计算下次执行时间
            LocalDateTime nextExecuteTime = schedulerService.getNextExecuteTime(task.getCronExpression());
            
            if (nextExecuteTime != null) {
                // 更新任务的下次执行时间
                task.setNextExecuteTime(nextExecuteTime);
                task.setLastExecuteTime(LocalDateTime.now());
                
                // 保存到数据库
                updateById(task);
                
                log.debug("任务 {} 下次执行时间已更新为: {}", task.getTaskName(), nextExecuteTime);
            } else {
                log.warn("无法计算任务 {} 的下次执行时间，Cron表达式: {}", task.getTaskName(), task.getCronExpression());
            }
        } catch (Exception e) {
            log.error("更新任务 {} 下次执行时间失败", task.getTaskName(), e);
            throw new RuntimeException("更新下次执行时间失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新任务统计信息
     * 
     * @param task 查询任务
     * @param success 是否成功
     */
    private void updateTaskStatistics(QueryTask task, boolean success) {
        try {
            int executeCount = task.getExecuteCount() != null ? task.getExecuteCount() + 1 : 1;
            int successCount = task.getSuccessCount() != null ? task.getSuccessCount() : 0;
            int failureCount = task.getFailureCount() != null ? task.getFailureCount() : 0;
            
            if (success) {
                successCount++;
            } else {
                failureCount++;
            }
            
            LocalDateTime now = LocalDateTime.now();
            String status = success ? "RUNNING" : "ERROR";
            
            baseMapper.updateExecuteInfo(
                task.getId(),
                status,
                now,
                null, // 下次执行时间由调度器计算
                executeCount,
                successCount,
                failureCount
            );
            
        } catch (Exception e) {
            log.error("更新任务统计信息失败", e);
        }
    }

    /**
     * 获取启用的查询任务
     * 
     * @return 查询任务列表
     */
    public List<QueryTask> getEnabledTasks() {
        QueryWrapper<QueryTask> wrapper = new QueryWrapper<>();
        wrapper.eq("enabled", true)
               .eq("deleted", false)
               .orderByDesc("create_time");
        return list(wrapper);
    }

    /**
     * 验证任务配置（保存时使用，不连接数据库）
     * 
     * @param task 查询任务
     * @return 验证结果
     */
    public Map<String, Object> validateTask(QueryTask task) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证数据库配置
            DatabaseConfig dbConfig = databaseConfigService.getById(task.getDatabaseConfigId());
            if (dbConfig == null || !dbConfig.getEnabled()) {
                result.put("valid", false);
                result.put("message", "数据库配置不存在或已禁用");
                return result;
            }
            
            // 验证告警模板（如果使用自定义模板则跳过校验）
            if (task.getCustomTemplateData() == null || task.getCustomTemplateData().isEmpty()) {
                // 使用现有模板时才校验
                AlertTemplate template = alertTemplateService.getById(task.getAlertTemplateId());
                if (template == null || !template.getEnabled()) {
                    result.put("valid", false);
                    result.put("message", "告警模板不存在或已禁用");
                    return result;
                }
            }
            
            // 基本SQL语句检查（不连接数据库）
            if (task.getSqlContent() == null || task.getSqlContent().trim().isEmpty()) {
                result.put("valid", false);
                result.put("message", "SQL语句不能为空");
                return result;
            }
            

            
            result.put("valid", true);
            result.put("message", "任务配置验证成功");
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "验证失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 验证任务配置并测试SQL（测试时使用，会连接数据库）
     * 
     * @param task 查询任务
     * @return 验证结果
     */
    public Map<String, Object> validateTaskWithSql(QueryTask task) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 先进行基本验证
            Map<String, Object> basicResult = validateTask(task);
            if (!(Boolean) basicResult.get("valid")) {
                return basicResult;
            }
            
            // 获取数据库配置
            DatabaseConfig dbConfig = databaseConfigService.getById(task.getDatabaseConfigId());
            
            // 验证SQL语句（连接数据库）
            Map<String, Object> sqlResult = databaseService.validateSql(dbConfig, task.getSqlContent());
            if (!(Boolean) sqlResult.get("valid")) {
                result.put("valid", false);
                result.put("message", "SQL验证失败: " + sqlResult.get("message"));
                return result;
            }
            
            result.put("valid", true);
            result.put("message", "任务配置和SQL验证成功");
            result.put("sqlResult", sqlResult);
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "验证失败: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * 测试发送钉钉消息 - 复用executeTask的核心逻辑
     */
    public boolean testSendDingTalk(QueryTask task) {
        try {
            // 生成测试执行ID
            String executionId = "TEST_" + snowflakeIdGenerator.nextId();
            
            // 获取数据库配置
            DatabaseConfig databaseConfig = databaseConfigService.getById(task.getDatabaseConfigId());
            if (databaseConfig == null) {
                throw new RuntimeException("数据库配置不存在");
            }

            // 记录SQL执行开始时间
            long sqlStartTime = System.currentTimeMillis();
            
            // 执行SQL查询（与executeTask保持一致）
            List<Map<String, Object>> queryResults = databaseService.executeQuery(databaseConfig, task.getSqlContent());
            
            // 计算SQL执行时间
            long sqlExecuteDuration = System.currentTimeMillis() - sqlStartTime;

            // 检查是否配置了告警模板（与executeTask保持一致的逻辑）
            AlertTemplate template = null;
            if (task.getAlertTemplateId() != null) {
                template = alertTemplateService.getById(task.getAlertTemplateId());
            }
            
            if (template != null) {
                // 如果配置了告警模板，使用模板发送（与executeTask一致）
                boolean shouldAlert = false;
                
                if (task.getAlertCondition() == null || task.getAlertCondition().isEmpty()) {
                    // 没有设置告警条件，直接发送（测试模式）
                    shouldAlert = true;
                } else {
                    // 有告警条件，进行判断（与executeTask一致）
                    shouldAlert = shouldTriggerAlert(task, queryResults);
                }
                
                if (shouldAlert) {
                    // 使用与executeTask相同的发送逻辑
                    String alertResult = sendDingTalkMessage(template, queryResults, task, sqlExecuteDuration, executionId);
                    return alertResult != null && !alertResult.contains("失败") && !alertResult.contains("错误");
                } else {
                    // 测试模式下，即使条件不满足也发送测试消息
                    String testMessage = buildTestMessageWithTemplate(task, queryResults, template, "告警条件不满足，但这是测试发送");
                    return sendTestMessage(task, testMessage);
                }
            } else {
                // 没有配置告警模板，发送简单的测试消息
                String testMessage = buildTestMessage(task, queryResults);
                return sendTestMessage(task, testMessage);
            }
        } catch (Exception e) {
            log.error("测试发送钉钉消息失败", e);
            return false;
        }
    }

    /**
     * 构建简单的测试消息（没有配置告警模板时使用）
     */
    private String buildTestMessage(QueryTask task, List<Map<String, Object>> queryResults) {
        StringBuilder message = new StringBuilder();
        message.append("【测试发送】\n");
        message.append("任务名称：").append(task.getTaskName()).append("\n");
        message.append("测试时间：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        message.append("查询结果数量：").append(queryResults.size()).append(" 条\n");

        // 如果有查询结果，显示前几条数据作为示例
        if (!queryResults.isEmpty()) {
            message.append("\n查询结果示例：\n");
            int maxRows = Math.min(3, queryResults.size()); // 最多显示3条
            for (int i = 0; i < maxRows; i++) {
                Map<String, Object> row = queryResults.get(i);
                message.append("第").append(i + 1).append("条：");
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    message.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                }
                // 移除最后的逗号和空格
                if (message.length() > 2) {
                    message.setLength(message.length() - 2);
                }
                message.append("\n");
            }

            if (queryResults.size() > maxRows) {
                message.append("...(共").append(queryResults.size()).append("条数据)\n");
            }
        }

        message.append("\n此为测试消息，实际告警将根据配置的告警条件触发。");
        return message.toString();
    }

    /**
     * 构建带模板的测试消息
     */
    private String buildTestMessageWithTemplate(QueryTask task, List<Map<String, Object>> queryResults, AlertTemplate template, String additionalInfo) {
        StringBuilder message = new StringBuilder();
        message.append("【测试发送】\n");
        message.append("任务名称：").append(task.getTaskName()).append("\n");
        message.append("测试时间：").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        message.append("使用模板：").append(template.getTemplateName()).append("\n");
        message.append("查询结果数量：").append(queryResults.size()).append(" 条\n");
        if (additionalInfo != null) {
            message.append("说明：").append(additionalInfo).append("\n");
        }
        message.append("\n此为测试消息，实际告警将根据配置的告警条件和模板触发。");
        return message.toString();
    }

    /**
     * 发送测试消息的通用方法
     */
    private boolean sendTestMessage(QueryTask task, String testMessage) {
        try {
            // 处理@人员手机号
            List<String> atMobiles = null;
            if (task.getAtMobiles() != null && !task.getAtMobiles().trim().isEmpty()) {
                atMobiles = Arrays.asList(task.getAtMobiles().split(","));
            }

            // 发送钉钉消息（与executeTask使用相同的发送逻辑）
            if (task.getDingtalkConfigId() != null) {
                // 使用现有钉钉配置
                DingTalkConfig dingTalkConfig = dingTalkService.getById(task.getDingtalkConfigId());
                if (dingTalkConfig == null) {
                    throw new RuntimeException("钉钉配置不存在");
                }
                String result = dingTalkService.sendMessage(dingTalkConfig, testMessage, task.getAtAll(), atMobiles);
                return result != null && !result.contains("失败") && !result.contains("错误");
            } else if (task.getCustomDingtalkData() != null) {
                // 使用自定义钉钉配置
                Map<String, Object> customData = task.getCustomDingtalkData();
                String webhookUrl = (String) customData.get("webhookUrl");
                String secret = (String) customData.get("secret");
                
                if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                    throw new RuntimeException("钉钉Webhook URL不能为空");
                }
                
                String result = dingTalkService.sendMessage(webhookUrl, secret, testMessage, task.getAtAll(), atMobiles);
                return result != null && !result.contains("失败") && !result.contains("错误");
            } else {
                throw new RuntimeException("未配置钉钉发送方式");
            }
        } catch (Exception e) {
            log.error("发送测试消息失败", e);
            return false;
        }
    }

    private boolean shouldTriggerAlert(QueryTask task, List<Map<String, Object>> queryResults) {
        // 如果没有设置告警条件，则不触发告警
        if (task.getAlertCondition() == null || task.getAlertCondition().isEmpty()) {
            return false;
        }

        // 如果没有设置阈值，则不触发告警
        if (task.getThreshold() == null) {
            return false;
        }

        String alertCondition = task.getAlertCondition();
        Integer threshold = task.getThreshold();
        String thresholdMode = task.getThresholdMode(); // "count" 或 "value"

        if ("count".equals(thresholdMode)) {
            // 按行数判断
            int rowCount = queryResults.size();
            return compareValues(rowCount, threshold, alertCondition);
        } else if ("value".equals(thresholdMode)) {
            // 按字段值判断
            String thresholdField = task.getThresholdField();
            if (thresholdField == null || thresholdField.isEmpty()) {
                log.warn("阈值字段名为空，无法进行字段值比较");
                return false;
            }

            // 取第一行数据的指定字段值进行比较
            if (!queryResults.isEmpty()) {
                Map<String, Object> firstRow = queryResults.get(0);
                Object fieldValue = firstRow.get(thresholdField);

                if (fieldValue == null) {
                    log.warn("字段 {} 的值为空，无法进行比较", thresholdField);
                    return false;
                }

                // 尝试将字段值转换为数字进行比较
                try {
                    double numericValue;
                    if (fieldValue instanceof Number) {
                        numericValue = ((Number) fieldValue).doubleValue();
                    } else {
                        numericValue = Double.parseDouble(fieldValue.toString());
                    }
                    return compareValues(numericValue, threshold.doubleValue(), alertCondition);
                } catch (NumberFormatException e) {
                    log.warn("字段 {} 的值 {} 无法转换为数字", thresholdField, fieldValue);
                    return false;
                }
            }
        }

        return false;
    }

    private boolean compareValues(double actualValue, double thresholdValue, String condition) {
        switch (condition) {
            case "gt":
                return actualValue > thresholdValue;
            case "gte":
                return actualValue >= thresholdValue;
            case "lt":
                return actualValue < thresholdValue;
            case "lte":
                return actualValue <= thresholdValue;
            case "eq":
                return actualValue == thresholdValue;
            case "ne":
                return actualValue != thresholdValue;
            default:
                log.warn("未知的告警条件: {}", condition);
                return false;
        }
}


}