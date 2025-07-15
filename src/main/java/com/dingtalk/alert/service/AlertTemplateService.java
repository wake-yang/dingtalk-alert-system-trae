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