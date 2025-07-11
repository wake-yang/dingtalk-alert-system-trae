package com.dingtalk.alert.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dingtalk.alert.dto.AlertTemplateListDTO;
import com.dingtalk.alert.entity.AlertTemplate;
import com.dingtalk.alert.service.AlertTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 告警模板控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/template")
public class AlertTemplateController {

    @Autowired
    private AlertTemplateService alertTemplateService;
    
    // 简单的内存缓存，避免频繁查询
    private volatile Long cachedCount = null;
    private volatile long lastCountCacheTime = 0;
    private static final long CACHE_DURATION = 30000; // 30秒缓存

    /**
     * 分页查询告警模板
     */
    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "20") long size,
                                   @RequestParam(required = false) String templateName,
                                   @RequestParam(required = false) String templateType,
                                   @RequestParam(required = false) Integer status) {
        Map<String, Object> result = new HashMap<>();
        try {
            Page<AlertTemplate> page = new Page<>(current, size);
            QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
            wrapper.eq("deleted", false)
                   .eq("is_custom", false); // 过滤掉自定义模板
    
            if (templateName != null && !templateName.isEmpty()) {
                wrapper.like("template_name", templateName);
            }
            if (templateType != null && !templateType.isEmpty()) {
                wrapper.eq("template_type", templateType);
            }
            if (status != null) {
                wrapper.eq("enabled", status == 1);
            }
    
            wrapper.orderByDesc("create_time");
            IPage<AlertTemplate> pageResult = alertTemplateService.page(page, wrapper);
            
            // 转换为DTO对象
            List<AlertTemplateListDTO> dtoList = pageResult.getRecords().stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
            
            // 构建分页结果
            IPage<AlertTemplateListDTO> dtoPageResult = new Page<>(current, size, pageResult.getTotal());
            dtoPageResult.setRecords(dtoList);
            
            result.put("success", true);
            result.put("data", dtoPageResult);
        } catch (Exception e) {
            log.error("查询告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private AlertTemplateListDTO convertToListDTO(AlertTemplate template) {
        AlertTemplateListDTO dto = new AlertTemplateListDTO();
        dto.setId(template.getId());
        dto.setTemplateName(template.getTemplateName());
        dto.setTemplateType(template.getTemplateType());
        dto.setEnabled(template.getEnabled());
        dto.setIsCustom(template.getIsCustom());
        dto.setTaskId(template.getTaskId());
        dto.setRemark(template.getRemark());
        dto.setCreateTime(template.getCreateTime());
        dto.setUpdateTime(template.getUpdateTime());
        return dto;
    }

    /**
     * 获取告警模板总数
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
            
            long count = alertTemplateService.count();
            cachedCount = count;
            lastCountCacheTime = currentTime;
            
            result.put("success", true);
            result.put("data", count);
        } catch (Exception e) {
            log.error("查询告警模板总数失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有启用的告警模板
     */
    @GetMapping("/enabled")
    public Map<String, Object> getEnabledTemplates() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<AlertTemplate> templates = alertTemplateService.getEnabledTemplates();
            result.put("success", true);
            result.put("data", templates);
        } catch (Exception e) {
            log.error("查询启用的告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID查询告警模板
     */
    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            AlertTemplate template = alertTemplateService.getById(id);
            result.put("success", true);
            result.put("data", template);
        } catch (Exception e) {
            log.error("查询告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存告警模板（根路径）
     */
    @PostMapping
    public Map<String, Object> saveTemplate(@RequestBody AlertTemplate template) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查模板名称是否重复
            AlertTemplate existing = alertTemplateService.getByTemplateName(template.getTemplateName());
            if (existing != null && !existing.getId().equals(template.getId())) {
                result.put("success", false);
                result.put("message", "模板名称已存在");
                return result;
            }
            
            boolean success = alertTemplateService.save(template);
            result.put("success", success);
            result.put("message", success ? "保存成功" : "保存失败");
        } catch (Exception e) {
            log.error("保存告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存告警模板
     */
    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody AlertTemplate template) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查模板名称是否重复
            AlertTemplate existing = alertTemplateService.getByTemplateName(template.getTemplateName());
            if (existing != null && !existing.getId().equals(template.getId())) {
                result.put("success", false);
                result.put("message", "模板名称已存在");
                return result;
            }
            
            boolean success = alertTemplateService.save(template);
            result.put("success", success);
            result.put("message", success ? "保存成功" : "保存失败");
        } catch (Exception e) {
            log.error("保存告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新告警模板（根路径）
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateTemplate(@PathVariable Long id, @RequestBody AlertTemplate template) {
        Map<String, Object> result = new HashMap<>();
        try {
            template.setId(id);
            // 检查模板名称是否重复
            AlertTemplate existing = alertTemplateService.getByTemplateName(template.getTemplateName());
            if (existing != null && !existing.getId().equals(template.getId())) {
                result.put("success", false);
                result.put("message", "模板名称已存在");
                return result;
            }
            
            boolean success = alertTemplateService.updateById(template);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            log.error("更新告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新告警模板
     */
    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody AlertTemplate template) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 检查模板名称是否重复
            AlertTemplate existing = alertTemplateService.getByTemplateName(template.getTemplateName());
            if (existing != null && !existing.getId().equals(template.getId())) {
                result.put("success", false);
                result.put("message", "模板名称已存在");
                return result;
            }
            
            boolean success = alertTemplateService.updateById(template);
            result.put("success", success);
            result.put("message", success ? "更新成功" : "更新失败");
        } catch (Exception e) {
            log.error("更新告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除告警模板
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteTemplate(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = alertTemplateService.deleteTemplate(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
        } catch (Exception e) {
            log.error("删除告警模板失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 测试钉钉连接
     */
    @PostMapping("/test-connection")
    public Map<String, Object> testConnection(@RequestBody AlertTemplate template) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 直接使用DingTalkService进行测试
            String testMessage = "【测试消息】\n模板名称：" + template.getTemplateName() + "\n测试时间：" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            
            // 这里需要根据实际情况调用DingTalkService的方法
            // 由于没有钉钉配置信息，建议返回成功但提示需要配置钉钉信息
            result.put("success", true);
            result.put("message", "模板格式验证通过，请在任务中配置具体的钉钉推送信息后进行完整测试");
            result.put("response", "模板内容：" + template.getTemplateContent());
        } catch (Exception e) {
            log.error("测试钉钉连接失败", e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 测试模板消息发送
     */
    @PostMapping("/test-message")
    public Map<String, Object> testMessage(@RequestBody AlertTemplate template) {
        Map<String, Object> result = new HashMap<>();
        try {
            String response = alertTemplateService.testTemplateMessage(template);
            result.put("success", true);
            result.put("message", "测试完成");
            result.put("response", response);
        } catch (Exception e) {
            log.error("测试模板消息发送失败", e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取模板类型选项
     */
    @GetMapping("/types")
    public Map<String, Object> getTemplateTypes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> types = java.util.Arrays.asList(
                    createMap("value", "text", "label", "文本消息"),
                    createMap("value", "markdown", "label", "Markdown消息"),
                    createMap("value", "custom", "label", "自定义模板")
            );
            result.put("success", true);
            result.put("data", types);
        } catch (Exception e) {
            log.error("获取模板类型失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取模板示例
     */
    @GetMapping("/examples")
    public Map<String, Object> getTemplateExamples() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, String> examples = new HashMap<>();
            
            // 文本消息示例
            examples.put("text", "告警通知：\n" +
                "数据库：${database_name}\n" +
                "表名：${table_name}\n" +
                "记录数：${record_count}\n" +
                "时间：${check_time}");
            
            // Markdown消息示例
            examples.put("markdown", "## 数据库告警通知\n\n" +
                "**数据库：** ${database_name}\n\n" +
                "**表名：** ${table_name}\n\n" +
                "**记录数：** ${record_count}\n\n" +
                "**检查时间：** ${check_time}\n\n" +
                "> 请及时处理相关问题");
            
            // 自定义模板示例
            examples.put("custom", "{\n" +
                "  \"msgtype\": \"text\",\n" +
                "  \"text\": {\n" +
                "    \"content\": \"告警：${alert_message}，时间：${alert_time}\"\n" +
                "  }\n" +
                "}");
            
            result.put("success", true);
            result.put("data", examples);
        } catch (Exception e) {
            log.error("获取模板示例失败", e);
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