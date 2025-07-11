package com.dingtalk.alert.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.dingtalk.alert.entity.DingTalkConfig;
import com.dingtalk.alert.service.DingTalkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉配置控制器
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/dingtalk")
public class DingTalkController {

    @Autowired
    private DingTalkService dingTalkService;

    /**
     * 获取钉钉配置信息
     */
    @GetMapping
    public Map<String, Object> getDingTalkInfo() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("success", true);
            result.put("message", "钉钉配置服务正常");
            result.put("data", new HashMap<>());
        } catch (Exception e) {
            log.error("获取钉钉配置信息失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存钉钉配置
     */
    @PostMapping
    public Map<String, Object> saveDingTalkConfig(@RequestBody Map<String, Object> configData) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name = (String) configData.get("name");
            String webhookUrl = (String) configData.get("webhookUrl");
            String secret = (String) configData.get("secret");
            String description = (String) configData.get("description");
            
            if (name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "配置名称不能为空");
                return result;
            }
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Webhook地址不能为空");
                return result;
            }
            
            // 保存配置
            String configId = dingTalkService.saveConfig(name, webhookUrl, secret, description);
            result.put("success", true);
            result.put("message", "钉钉配置保存成功");

            Map<String, String> map = new HashMap<>();
            map.put("id",configId);
            result.put("data", map);
        } catch (Exception e) {
            log.error("保存钉钉配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 测试钉钉服务
     */
    @PostMapping("/test")
    public Map<String, Object> testDingTalkService(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String webhook = (String) params.get("webhookUrl");
            String secret = (String) params.get("secret");
            String message = (String) params.get("message");
            
            if (webhook == null || webhook.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Webhook地址不能为空");
                result.put("data", null);
                result.put("timestamp", System.currentTimeMillis());
                return result;
            }
            
            // 发送测试消息
            String testResult = dingTalkService.sendMessage(webhook, secret, message != null ? message : "钉钉服务测试消息");
            
            // 检查返回结果是否包含错误信息
            if (testResult != null && testResult.startsWith("发送失败:")) {
                result.put("success", false);
                result.put("message", testResult);
                result.put("data", testResult);
            } else {
                result.put("success", true);
                result.put("message", "钉钉服务测试成功");
                result.put("data", testResult);
            }
            result.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            log.error("测试钉钉服务失败", e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            result.put("data", null);
            result.put("timestamp", System.currentTimeMillis());
        }
        return result;
    }

    /**
     * 测试钉钉连接
     */
    @PostMapping("/test-connection")
    public Map<String, Object> testConnection(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String webhook = params.get("webhook");
            String secret = params.get("secret");
            
            if (webhook == null || webhook.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Webhook地址不能为空");
                return result;
            }
            
            String testResult = dingTalkService.testConnection(webhook, secret);
            result.put("success", true);
            result.put("data", testResult);
        } catch (Exception e) {
            log.error("测试钉钉连接失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 发送测试消息
     */
    @PostMapping("/send-test-message")
    public Map<String, Object> sendTestMessage(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String webhook = (String) params.get("webhook");
            String secret = (String) params.get("secret");
            String messageType = (String) params.get("messageType");
            String content = (String) params.get("content");
            Boolean atAll = (Boolean) params.get("atAll");
            @SuppressWarnings("unchecked")
            List<String> atMobiles = (List<String>) params.get("atMobiles");
            
            if (webhook == null || webhook.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Webhook地址不能为空");
                return result;
            }
            
            if (content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "消息内容不能为空");
                return result;
            }
            
            String sendResult;
            
            switch (messageType != null ? messageType : "text") {
                case "markdown":
                    sendResult = dingTalkService.sendMarkdownMessage(
                        webhook, secret, "测试消息", content, atAll, atMobiles);
                    break;
                case "text":
                default:
                    sendResult = dingTalkService.sendTextMessage(
                        webhook, secret, content, atAll, atMobiles);
                    break;
            }
            
            result.put("success", true);
            result.put("data", sendResult);
            result.put("message", "测试消息发送成功");
        } catch (Exception e) {
            log.error("发送测试消息失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 发送自定义模板消息
     */
    @PostMapping("/send-template-message")
    public Map<String, Object> sendTemplateMessage(@RequestBody Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String webhook = (String) params.get("webhook");
            String secret = (String) params.get("secret");
            String template = (String) params.get("template");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) params.get("data");
            Boolean atAll = (Boolean) params.get("atAll");
            @SuppressWarnings("unchecked")
            List<String> atMobiles = (List<String>) params.get("atMobiles");
            
            if (webhook == null || webhook.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Webhook地址不能为空");
                return result;
            }
            
            if (template == null || template.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "模板内容不能为空");
                return result;
            }

            String sendResult = dingTalkService.sendTemplateMessage(
                webhook, secret, template, data, atAll, atMobiles);
            
            result.put("success", true);
            result.put("data", sendResult);
            result.put("message", "模板消息发送成功");
        } catch (Exception e) {
            log.error("发送模板消息失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取消息类型选项
     */
    @GetMapping("/message-types")
    public Map<String, Object> getMessageTypes() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> types = java.util.Arrays.asList(
                createMap("value", "text", "label", "文本消息"),
                createMap("value", "markdown", "label", "Markdown消息")
            );
            result.put("success", true);
            result.put("data", types);
        } catch (Exception e) {
            log.error("获取消息类型选项失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取钉钉配置示例
     */
    @GetMapping("/config-examples")
    public Map<String, Object> getConfigExamples() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> examples = new HashMap<>();

            // Webhook示例
            examples.put("webhook", "https://oapi.dingtalk.com/robot/send?access_token=YOUR_ACCESS_TOKEN");

            // 密钥示例
            examples.put("secret", "SEC1234567890abcdef...");

            // 文本消息模板示例
            examples.put("textTemplate", "告警通知：\n任务名称：${taskName}\n执行时间：${executeTime}\n执行结果：${result}");

            // Markdown消息模板示例
            examples.put("markdownTemplate",
                "## 告警通知\n\n" +
                "**任务名称：** ${taskName}\n\n" +
                "**执行时间：** ${executeTime}\n\n" +
                "**执行结果：** ${result}\n\n" +
                "**详细信息：** ${details}");

            // 自定义模板示例
            examples.put("customTemplate",
                "数据库监控告警\n" +
                "数据库：${database}\n" +
                "表名：${tableName}\n" +
                "记录数：${count}\n" +
                "阈值：${threshold}\n" +
                "状态：${status}");

            result.put("success", true);
            result.put("data", examples);
        } catch (Exception e) {
            log.error("获取钉钉配置示例失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 验证Webhook地址格式
     */
    @PostMapping("/validate-webhook")
    public Map<String, Object> validateWebhook(@RequestBody Map<String, String> params) {
        Map<String, Object> result = new HashMap<>();
        try {
            String webhook = params.get("webhook");

            if (webhook == null || webhook.trim().isEmpty()) {
                result.put("valid", false);
                result.put("message", "Webhook地址不能为空");
            } else if (!webhook.startsWith("https://oapi.dingtalk.com/robot/send")) {
                result.put("valid", false);
                result.put("message", "Webhook地址格式不正确");
            } else if (!webhook.contains("access_token=")) {
                result.put("valid", false);
                result.put("message", "Webhook地址缺少access_token参数");
            } else {
                result.put("valid", true);
                result.put("message", "Webhook地址格式正确");
            }

            result.put("success", true);
        } catch (Exception e) {
            log.error("验证Webhook地址失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取钉钉机器人配置说明
     */
    @GetMapping("/robot-guide")
    public Map<String, Object> getRobotGuide() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, String>> steps = java.util.Arrays.asList(
                createMap(
                    "step", "1",
                    "title", "创建钉钉群",
                    "description", "在钉钉中创建一个群聊，用于接收告警消息"
                ),
                createMap(
                    "step", "2",
                    "title", "添加机器人",
                    "description", "在群设置中选择'智能群助手' -> '添加机器人' -> '自定义机器人'"
                ),
                createMap(
                    "step", "3",
                    "title", "配置安全设置",
                    "description", "选择'加签'安全设置，复制生成的密钥"
                ),
                createMap(
                    "step", "4",
                    "title", "获取Webhook",
                    "description", "复制生成的Webhook地址"
                ),
                createMap(
                    "step", "5",
                    "title", "配置系统",
                    "description", "将Webhook地址和密钥配置到本系统中"
                )
            );
            
            result.put("success", true);
            result.put("data", steps);
        } catch (Exception e) {
            log.error("获取钉钉机器人配置说明失败", e);
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

    /**
     * 获取全局钉钉配置
     */
    @GetMapping("/global")
    public Map<String, Object> getGlobalConfig() {
        Map<String, Object> result = new HashMap<>();
        try {
            DingTalkConfig globalConfig = dingTalkService.getGlobalConfig();
            if (globalConfig != null) {
                Map<String, Object> config = new HashMap<>();
                config.put("id", globalConfig.getId());
                config.put("name", globalConfig.getName());
                config.put("webhookUrl", globalConfig.getWebhookUrl());
                config.put("secret", globalConfig.getSecret());
                config.put("description", globalConfig.getDescription());
                config.put("enabled", globalConfig.getEnabled());
                result.put("data", config);
            } else {
                // 如果没有全局配置，返回默认值
                Map<String, Object> config = new HashMap<>();
                config.put("webhookUrl", "");
                config.put("secret", "");
                config.put("enabled", false);
                result.put("data", config);
            }
            result.put("success", true);
        } catch (Exception e) {
            log.error("获取全局钉钉配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 保存全局钉钉配置
     */
    @PostMapping("/global")
    public Map<String, Object> saveGlobalConfig(@RequestBody Map<String, Object> config) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name = (String) config.get("name");
            String webhookUrl = (String) config.get("webhookUrl");
            String secret = (String) config.get("secret");
            String description = (String) config.get("description");
            Boolean enabled = (Boolean) config.get("enabled");
            
            if (name == null || name.trim().isEmpty()) {
                name = "全局配置";
            }
            
            String configId = dingTalkService.saveOrUpdateGlobalConfig(name, webhookUrl, secret, description, enabled);
            
            result.put("success", true);
            result.put("message", "全局配置保存成功");
            Map<String, String> data = new HashMap<>();
            data.put("id", configId);
            result.put("data", data);
        } catch (Exception e) {
            log.error("保存全局钉钉配置失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 分页获取钉钉配置列表
     */
    @GetMapping("/list")
    public Map<String, Object> getConfigList(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        Map<String, Object> result = new HashMap<>();
        try {
            IPage<DingTalkConfig> pageResult = dingTalkService.getConfigList(current, size);
            result.put("success", true);
            result.put("data", pageResult);
        } catch (Exception e) {
            log.error("获取钉钉配置列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取所有钉钉配置（用于下拉框）
     */
    @GetMapping("/configs")
    public Map<String, Object> getAllConfigs() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DingTalkConfig> configs = dingTalkService.getAllConfigs();
            result.put("success", true);
            result.put("data", configs);
            log.info("获取钉钉配置列表成功，共{}条记录", configs.size());
        } catch (Exception e) {
            log.error("获取钉钉配置列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 根据ID获取钉钉配置
     */
    @GetMapping("/{id}")
    public Map<String, Object> getConfigById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            DingTalkConfig config = dingTalkService.getConfigById(id);
            if (config != null) {
                result.put("success", true);
                result.put("data", config);
                log.info("获取钉钉配置成功，ID: {}", id);
            } else {
                result.put("success", false);
                result.put("message", "配置不存在");
            }
        } catch (Exception e) {
            log.error("获取钉钉配置失败，ID: {}", id, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 测试指定ID的钉钉配置
     */
    @PostMapping("/{id}/test")
    public Map<String, Object> testConfigById(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 根据ID获取配置
            DingTalkConfig config = dingTalkService.getConfigById(id);
            if (config == null) {
                result.put("success", false);
                result.put("message", "配置不存在");
                return result;
            }
            
            // 检查配置是否启用
            if (config.getEnabled() != null && !config.getEnabled()) {
                result.put("success", false);
                result.put("message", "配置已禁用，无法测试");
                return result;
            }
            
            // 发送测试消息
            String testMessage = "【钉钉配置测试】\n配置名称：" + config.getName() + 
                           "\n测试时间：" + java.time.LocalDateTime.now().format(
                           java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            String testResult = dingTalkService.sendMessage(config, testMessage, false, null);
            
            // 检查返回结果
            if (testResult != null && testResult.startsWith("发送失败:")) {
                result.put("success", false);
                result.put("message", testResult);
                result.put("data", testResult);
            } else {
                result.put("success", true);
                result.put("message", "测试消息发送成功");
                result.put("data", testResult);
            }
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("测试钉钉配置完成，ID: {}, 配置名称: {}", id, config.getName());
        } catch (Exception e) {
            log.error("测试钉钉配置失败，ID: {}", id, e);
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            result.put("data", null);
            result.put("timestamp", System.currentTimeMillis());
        }
        return result;
    }

    private Map<String, String> createMap(String key1, String value1, String key2, String value2, String key3, String value3) {
        Map<String, String> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        return map;
    }
    /**
     * 删除钉钉配置
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteConfig(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = dingTalkService.deleteConfig(id);
            result.put("success", success);
            result.put("message", success ? "删除成功" : "删除失败");
            log.info("删除钉钉配置请求处理完成，ID: {}, 结果: {}", id, success);
        } catch (Exception e) {
            log.error("删除钉钉配置失败，ID: {}", id, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
    /**
     * 更新钉钉配置
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateConfig(@PathVariable String id, @RequestBody Map<String, Object> configData) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 设置ID到配置数据中
            configData.put("id", id);
            
            String name = (String) configData.get("name");
            String webhookUrl = (String) configData.get("webhookUrl");
            String secret = (String) configData.get("secret");
            String description = (String) configData.get("description");
            Boolean enabled = (Boolean) configData.get("enabled");
            Boolean isDefault = (Boolean) configData.get("isDefault");
            
            if (name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "配置名称不能为空");
                return result;
            }
            
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Webhook地址不能为空");
                return result;
            }
            
            // 更新配置
            DingTalkConfig config = dingTalkService.getConfigById(id);
            if (config == null) {
                result.put("success", false);
                result.put("message", "配置不存在");
                return result;
            }
            
            config.setName(name.trim());
            config.setWebhookUrl(webhookUrl.trim());
            config.setSecret(secret);
            config.setDescription(description);
            config.setEnabled(enabled != null ? enabled : false);
            
            boolean success = dingTalkService.updateById(config);
            
            if (success) {
                result.put("success", true);
                result.put("message", "配置更新成功");
                result.put("data", config);
                log.info("更新钉钉配置成功，ID: {}, 配置名称: {}", id, name);
            } else {
                result.put("success", false);
                result.put("message", "配置更新失败");
            }
        } catch (Exception e) {
            log.error("更新钉钉配置失败，ID: {}", id, e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}