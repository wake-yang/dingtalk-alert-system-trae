package com.dingtalk.alert.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dingtalk.alert.entity.AlertTemplate;
import com.dingtalk.alert.mapper.AlertTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 告警模板服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class AlertTemplateService extends ServiceImpl<AlertTemplateMapper, AlertTemplate> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DingTalkService dingTalkService;

    /**
     * 保存告警模板
     * 
     * @param template 告警模板
     * @return 保存结果
     */
    @Override
    @Transactional
    public boolean save(AlertTemplate template) {
        // 验证模板名称唯一性
        if (existsByTemplateName(template.getTemplateName())) {
            throw new RuntimeException("模板名称已存在: " + template.getTemplateName());
        }
        
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(false);
        
        // 设置默认值
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (template.getIsCustom() == null) {
            template.setIsCustom(false);
        }
        
        return super.save(template);
    }

    /**
     * 更新告警模板
     * 
     * @param template 告警模板
     * @return 更新结果
     */
    @Override
    @Transactional
    public boolean updateById(AlertTemplate template) {
        // 验证模板名称唯一性（排除自身）
        AlertTemplate existing = getByTemplateName(template.getTemplateName());
        if (existing != null && !existing.getId().equals(template.getId())) {
            throw new RuntimeException("模板名称已存在: " + template.getTemplateName());
        }
        
        template.setUpdateTime(LocalDateTime.now());
        return super.updateById(template);
    }

    /**
     * 获取启用的告警模板
     * 
     * @return 告警模板列表
     */
    public List<AlertTemplate> getEnabledTemplates() {
        QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("enabled", true)
               .eq("deleted", false)
               .eq("is_custom", false)  // 过滤掉自定义模板
               .orderByDesc("create_time");
        return list(wrapper);
    }

    /**
     * 获取所有公共模板（非自定义模板）
     * 
     * @return 告警模板列表
     */
    public List<AlertTemplate> getPublicTemplates() {
        QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false)
               .eq("is_custom", false)
               .orderByDesc("create_time");
        return list(wrapper);
    }

    /**
     * 根据任务ID获取自定义模板
     * 
     * @param taskId 任务ID
     * @return 自定义模板
     */
    public AlertTemplate getCustomTemplateByTaskId(Long taskId) {
        QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId)
               .eq("is_custom", true)
               .eq("deleted", false);
        return getOne(wrapper);
    }

    /**
     * 根据模板名称查询
     * 
     * @param templateName 模板名称
     * @return 告警模板
     */
    public AlertTemplate getByTemplateName(String templateName) {
        if (!StringUtils.hasText(templateName)) {
            return null;
        }
        QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_name", templateName)
               .eq("deleted", false);
        return getOne(wrapper);
    }

    /**
     * 检查模板名称是否存在
     * 
     * @param templateName 模板名称
     * @return 是否存在
     */
    public boolean existsByTemplateName(String templateName) {
        return getByTemplateName(templateName) != null;
    }

    /**
     * 验证模板内容格式
     * 
     * @param template 告警模板
     * @return 验证结果
     */
    public String validateTemplate(AlertTemplate template) {
        try {
            // 验证必填字段
            if (!StringUtils.hasText(template.getTemplateName())) {
                return "模板名称不能为空";
            }
            if (!StringUtils.hasText(template.getTemplateContent())) {
                return "模板内容不能为空";
            }
            if (!StringUtils.hasText(template.getTemplateType())) {
                return "模板类型不能为空";
            }

            // 验证模板类型
            if (!"text".equals(template.getTemplateType()) && 
                !"markdown".equals(template.getTemplateType()) && 
                !"template".equals(template.getTemplateType())) {
                return "模板类型必须是 text、markdown 或 template";
            }

            // 验证模板字段格式（如果有）
            if (StringUtils.hasText(template.getTemplateFields())) {
                try {
                    objectMapper.readValue(template.getTemplateFields(), new TypeReference<List<String>>(){});
                } catch (Exception e) {
                    return "模板字段格式错误，必须是有效的JSON数组";
                }
            }

            // 验证模板内容中的变量
            String content = template.getTemplateContent();
            if ("template".equals(template.getTemplateType()) && StringUtils.hasText(template.getTemplateFields())) {
                List<String> fields = objectMapper.readValue(template.getTemplateFields(), new TypeReference<List<String>>(){});
                for (String field : fields) {
                    if (!content.contains("${" + field + "}")) {
                        log.warn("模板内容中缺少字段变量: ${{}}", field);
                    }
                }
            }

            return "验证通过";
        } catch (Exception e) {
            log.error("验证模板失败", e);
            return "验证失败: " + e.getMessage();
        }
    }

    /**
     * 复制模板
     * 
     * @param sourceId 源模板ID
     * @param newTemplateName 新模板名称
     * @return 复制结果
     */
    @Transactional
    public AlertTemplate copyTemplate(Long sourceId, String newTemplateName) {
        AlertTemplate source = getById(sourceId);
        if (source == null) {
            throw new RuntimeException("源模板不存在");
        }
        
        if (existsByTemplateName(newTemplateName)) {
            throw new RuntimeException("新模板名称已存在: " + newTemplateName);
        }

        AlertTemplate newTemplate = new AlertTemplate();
        newTemplate.setTemplateName(newTemplateName);
        newTemplate.setTemplateContent(source.getTemplateContent());
        newTemplate.setTemplateFields(source.getTemplateFields());
        newTemplate.setTemplateType(source.getTemplateType());
        newTemplate.setEnabled(true);
        newTemplate.setIsCustom(false);
        newTemplate.setRemark("复制自: " + source.getTemplateName());
        
        if (save(newTemplate)) {
            return newTemplate;
        }
        throw new RuntimeException("复制模板失败");
    }

    /**
     * 批量启用/禁用模板
     * 
     * @param ids 模板ID列表
     * @param enabled 启用状态
     * @return 更新结果
     */
    @Transactional
    public boolean batchUpdateEnabled(List<Long> ids, boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        
        QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
        wrapper.in("id", ids)
               .eq("deleted", false);
        
        AlertTemplate updateTemplate = new AlertTemplate();
        updateTemplate.setEnabled(enabled);
        updateTemplate.setUpdateTime(LocalDateTime.now());
        
        return update(updateTemplate, wrapper);
    }

    /**
     * 获取模板使用统计
     * 
     * @param templateId 模板ID
     * @return 使用统计信息
     */
    public Map<String, Object> getTemplateUsageStats(Long templateId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 这里可以添加统计逻辑，比如:
        // - 使用该模板的任务数量
        // - 最近使用时间
        // - 发送消息次数等
        
        stats.put("templateId", templateId);
        stats.put("taskCount", 0); // 需要关联查询task表
        stats.put("lastUsedTime", null);
        
        return stats;
    }

    /**
     * 删除告警模板（逻辑删除）
     * 
     * @param id 模板ID
     * @return 删除结果
     */
    @Transactional
    public boolean deleteTemplate(Long id) {
        AlertTemplate template = getById(id);
        if (template == null) {
            return false;
        }
        
        // 检查是否有任务在使用该模板
        if (template.getIsCustom() != null && template.getIsCustom() && template.getTaskId() != null) {
            log.warn("尝试删除正在使用的自定义模板: {}", template.getTemplateName());
        }
        
        // 使用 MyBatis-Plus 的逻辑删除方法
        return removeById(id);
    }

    /**
     * 物理删除自定义模板
     * 
     * @param taskId 任务ID
     * @return 删除结果
     */
    @Transactional
    public boolean deleteCustomTemplateByTaskId(Long taskId) {
        QueryWrapper<AlertTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId)
               .eq("is_custom", true);
        return remove(wrapper);
    }

    /**
     * 测试模板消息发送
     *
     * @param template 告警模板
     * @return 发送结果
     */
    public String testTemplateMessage(AlertTemplate template) {
        // 注意：这里的webhookUrl和secret是用于测试的，实际应用中应从配置或任务中获取
        String webhookUrl = "https://oapi.dingtalk.com/robot/send?access_token=xxxxxxxx"; // 替换为你的测试机器人accessToken
        String secret = "SECxxxxxxxx"; // 替换为你的测试机器人secret

        try {
            // 构造测试数据
            Map<String, Object> data = new HashMap<>();
            if (StringUtils.hasText(template.getTemplateFields())) {
                List<String> fields = objectMapper.readValue(template.getTemplateFields(), new TypeReference<List<String>>() {});
                for (String field : fields) {
                    data.put(field, "[测试数据]" + field);
                }
            }
            data.put("currentTime", LocalDateTime.now().toString());

            // 发送消息
            return dingTalkService.sendTemplateMessage(webhookUrl, secret, template.getTemplateContent(), data, false, null);
        } catch (Exception e) {
            log.error("测试模板消息发送失败", e);
            throw new RuntimeException("测试模板消息发送失败: " + e.getMessage(), e);
        }
    }
}