package com.dingtalk.alert.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dingtalk.alert.entity.AlertTemplate;
import com.dingtalk.alert.entity.DingTalkConfig;
import com.dingtalk.alert.mapper.AlertTemplateMapper;
import com.dingtalk.alert.mapper.DingTalkConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 钉钉推送服务
 * 
 * @author system
 * @since 2024-01-01
 */
@Slf4j
@Service
public class DingTalkService  extends ServiceImpl<DingTalkConfigMapper, DingTalkConfig> {

    @Autowired
    private DingTalkConfigMapper dingTalkConfigMapper;

    /**
     * 发送文本消息
     * 
     * @param webhookUrl Webhook地址
     * @param secret 密钥
     * @param content 消息内容
     * @param atAll 是否@所有人
     * @param atMobiles @指定人员手机号
     * @return 推送结果
     */
    public String sendTextMessage(String webhookUrl, String secret, String content, 
                                 Boolean atAll, List<String> atMobiles) {
        try {
            // 构建消息体
            JSONObject message = new JSONObject();
            message.put("msgtype", "text");
            
            JSONObject text = new JSONObject();
            text.put("content", content);
            message.put("text", text);
            
            // 设置@信息
            if (atAll != null && atAll || (atMobiles != null && !atMobiles.isEmpty())) {
                JSONObject at = new JSONObject();
                if (atAll != null && atAll) {
                    at.put("isAtAll", true);
                }
                if (atMobiles != null && !atMobiles.isEmpty()) {
                    at.put("atMobiles", atMobiles);
                }
                message.put("at", at);
            }
            
            return sendMessage(webhookUrl, secret, message.toJSONString());
        } catch (Exception e) {
            log.error("发送钉钉文本消息失败", e);
            return "发送失败: " + e.getMessage();
        }
    }

    /**
     * 发送Markdown消息
     * 
     * @param webhookUrl Webhook地址
     * @param secret 密钥
     * @param title 标题
     * @param content Markdown内容
     * @param atAll 是否@所有人
     * @param atMobiles @指定人员手机号
     * @return 推送结果
     */
    public String sendMarkdownMessage(String webhookUrl, String secret, String title, 
                                     String content, Boolean atAll, List<String> atMobiles) {
        try {
            // 构建消息体
            JSONObject message = new JSONObject();
            message.put("msgtype", "markdown");
            
            JSONObject markdown = new JSONObject();
            markdown.put("title", title);
            markdown.put("text", content);
            message.put("markdown", markdown);
            
            // 设置@信息
            if (atAll != null && atAll || (atMobiles != null && !atMobiles.isEmpty())) {
                JSONObject at = new JSONObject();
                if (atAll != null && atAll) {
                    at.put("isAtAll", true);
                }
                if (atMobiles != null && !atMobiles.isEmpty()) {
                    at.put("atMobiles", atMobiles);
                }
                message.put("at", at);
            }
            
            return sendMessage(webhookUrl, secret, message.toJSONString());
        } catch (Exception e) {
            log.error("发送钉钉Markdown消息失败", e);
            return "发送失败: " + e.getMessage();
        }
    }

    /**
     * 发送自定义模板消息
     * 
     * @param webhookUrl Webhook地址
     * @param secret 密钥
     * @param templateContent 模板内容
     * @param data 数据
     * @param atAll 是否@所有人
     * @param atMobiles @指定人员手机号
     * @return 推送结果
     */
    public String sendTemplateMessage(String webhookUrl, String secret, String templateContent, 
                                     Map<String, Object> data, Boolean atAll, List<String> atMobiles) {
        try {
            // 替换模板中的占位符
            String content = templateContent;
            if (data != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    String placeholder = "${" + entry.getKey() + "}";
                    String value = entry.getValue() != null ? entry.getValue().toString() : "";
                    content = content.replace(placeholder, value);
                }
            }
            
            // 尝试解析为JSON，如果失败则作为文本消息发送
            try {
                JSONObject message = JSON.parseObject(content);
                
                // 设置@信息
                if (atAll != null && atAll || (atMobiles != null && !atMobiles.isEmpty())) {
                    JSONObject at = new JSONObject();
                    if (atAll != null && atAll) {
                        at.put("isAtAll", true);
                    }
                    if (atMobiles != null && !atMobiles.isEmpty()) {
                        at.put("atMobiles", atMobiles);
                    }
                    message.put("at", at);
                }
                
                return sendMessage(webhookUrl, secret, message.toJSONString());
            } catch (Exception e) {
                // 作为文本消息发送
                return sendTextMessage(webhookUrl, secret, content, atAll, atMobiles);
            }
        } catch (Exception e) {
            log.error("发送钉钉模板消息失败", e);
            return "发送失败: " + e.getMessage();
        }
    }

    /**
     * 发送消息（使用DingTalkConfig对象）
     * 
     * @param config 钉钉配置
     * @param message 消息内容
     * @param atAll 是否@所有人
     * @param atMobiles @指定人员手机号
     * @return 发送结果
     */
    public String sendMessage(DingTalkConfig config, String message, Boolean atAll, List<String> atMobiles) {
        if (config == null) {
            return "钉钉配置不能为空";
        }
        return sendTextMessage(config.getWebhookUrl(), config.getSecret(), message, atAll, atMobiles);
    }

    /**
     * 发送消息（使用自定义配置）
     * 
     * @param webhookUrl Webhook URL
     * @param secret 密钥
     * @param message 消息内容
     * @param atAll 是否@所有人
     * @param atMobiles @指定人员手机号
     * @return 发送结果
     */
    public String sendMessage(String webhookUrl, String secret, String message, Boolean atAll, List<String> atMobiles) {
        return sendTextMessage(webhookUrl, secret, message, atAll, atMobiles);
    }

    /**
     * 发送消息
     * 
     * @param webhookUrl Webhook URL
     * @param secret 密钥
     * @param messageBody 消息体
     * @return 发送结果
     */
    public String sendMessage(String webhookUrl, String secret, String messageBody) {
        CloseableHttpClient httpClient = null;
        CloseableHttpResponse response = null;
        
        try {
            // 生成签名
            String url = webhookUrl;
            if (secret != null && !secret.trim().isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String sign = generateSign(secret, timestamp);
                url = webhookUrl + "&timestamp=" + timestamp + "&sign=" + URLEncoder.encode(sign, "UTF-8");
            }
            
            // 输出请求报文日志
            log.info("钉钉推送请求URL: {}", url);
            log.info("钉钉推送请求体: {}", messageBody);
            
            httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);
            
            // 设置请求头
            httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
            log.info("钉钉推送请求头: Content-Type=application/json; charset=utf-8");
            
            // 设置请求体
            StringEntity entity = new StringEntity(messageBody, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            // 发送请求
            response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String result = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
            
            // 输出响应报文日志
            log.info("钉钉推送响应状态码: {}", response.getStatusLine().getStatusCode());
            log.info("钉钉推送响应报文: {}", result);
            return result;
            
        } catch (Exception e) {
            log.error("发送钉钉消息失败", e);
            return "发送失败: " + e.getMessage();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (Exception e) {
                log.error("关闭HTTP连接失败", e);
            }
        }
    }

    /**
     * 生成钉钉签名
     * 
     * @param secret 密钥
     * @param timestamp 时间戳
     * @return 签名
     */
    private String generateSign(String secret, long timestamp) throws Exception {
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signData);
    }

    /**
     * 测试钉钉连接
     * 
     * @param webhookUrl Webhook地址
     * @param secret 密钥
     * @return 测试结果
     */
    public String testConnection(String webhookUrl, String secret) {
        String testMessage = "钉钉告警系统连接测试 - " + java.time.LocalDateTime.now();
        return sendTextMessage(webhookUrl, secret, testMessage, false, null);
    }

    /**
     * 保存钉钉配置
     * 
     * @param name 配置名称
     * @param webhookUrl Webhook地址
     * @param secret 密钥
     * @param description 描述
     * @return 配置ID
     */
    public String saveConfig(String name, String webhookUrl, String secret, String description) {
        try {
            DingTalkConfig config = new DingTalkConfig();
            config.setName(name);
            config.setWebhookUrl(webhookUrl);
            config.setSecret(secret);
            config.setDescription(description);
            config.setEnabled(true);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            config.setCreateBy("system");
            config.setUpdateBy("system");
            
            int result = dingTalkConfigMapper.insert(config);
            if (result > 0) {
                log.info("保存钉钉配置成功: name={}, webhookUrl={}, description={}, configId={}", 
                        name, webhookUrl, description, config.getId());
                return String.valueOf(config.getId());
            } else {
                throw new RuntimeException("保存配置失败，数据库操作返回0");
            }
        } catch (Exception e) {
            log.error("保存钉钉配置失败", e);
            throw new RuntimeException("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 分页获取钉钉配置列表
     * 
     * @param current 当前页
     * @param size 页大小
     * @return 分页结果
     */
    public IPage<DingTalkConfig> getConfigList(long current, long size) {
        try {
            Page<DingTalkConfig> page = new Page<>(current, size);
            return dingTalkConfigMapper.selectPage(page, null);
        } catch (Exception e) {
            log.error("获取钉钉配置列表失败", e);
            throw new RuntimeException("获取配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有钉钉配置
     */
    public List<DingTalkConfig> getAllConfigs() {
        try {
            QueryWrapper<DingTalkConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("create_time");
            return dingTalkConfigMapper.selectList(queryWrapper);
        } catch (Exception e) {
            log.error("获取所有钉钉配置失败", e);
            throw new RuntimeException("获取配置列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取钉钉配置
     */
    public DingTalkConfig getConfigById(String id) {
        try {
            return dingTalkConfigMapper.selectById(id);
        } catch (Exception e) {
            log.error("根据ID获取钉钉配置失败，ID: {}", id, e);
            throw new RuntimeException("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取钉钉配置
     * 
     * @param id 配置ID
     * @return 钉钉配置
     */
    public DingTalkConfig getById(Long id) {
        log.info("Getting dingtalk config by id: {}", id);
        return dingTalkConfigMapper.selectById(id);
    }

    /**
     * 获取全局钉钉配置
     */
    public DingTalkConfig getGlobalConfig() {
        QueryWrapper<DingTalkConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", "全局配置").or().like("name", "global");
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT 1");
        return dingTalkConfigMapper.selectOne(queryWrapper);
    }

    /**
     * 保存或更新全局钉钉配置
     */
    public String saveOrUpdateGlobalConfig(String name, String webhookUrl, String secret, String description, Boolean enabled) {
        // 先查找是否已存在全局配置
        DingTalkConfig existingConfig = getGlobalConfig();
        
        if (existingConfig != null) {
            // 更新现有配置
            existingConfig.setName(name);
            existingConfig.setWebhookUrl(webhookUrl);
            existingConfig.setSecret(secret);
            existingConfig.setDescription(description);
            existingConfig.setEnabled(enabled != null ? enabled : false);
            existingConfig.setUpdateTime(LocalDateTime.now());
            existingConfig.setUpdateBy("system");
            
            int result = dingTalkConfigMapper.updateById(existingConfig);
            if (result > 0) {
                return existingConfig.getId().toString();
            } else {
                throw new RuntimeException("更新全局配置失败");
            }
        } else {
            // 创建新的全局配置
            DingTalkConfig config = new DingTalkConfig();
            // id字段是AUTO类型，不需要手动设置
            config.setName(name);
            config.setWebhookUrl(webhookUrl);
            config.setSecret(secret);
            config.setDescription(description);
            config.setEnabled(enabled != null ? enabled : false);
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            config.setCreateBy("system");
            config.setUpdateBy("system");
            
            int result = dingTalkConfigMapper.insert(config);
            if (result > 0) {
                return config.getId().toString();
            } else {
                throw new RuntimeException("保存全局配置失败");
            }
        }
    }

    /**
     * 删除钉钉配置
     * 
     * @param id 配置ID
     * @return 删除结果
     */
    public boolean deleteConfig(Long id) {
        try {
            DingTalkConfig config = this.getById(id);
            if (config == null) {
                log.warn("要删除的钉钉配置不存在，ID: {}", id);
                return false;
            }
            
            // 使用 MyBatis-Plus 的逻辑删除
            config.setDeleted(true);
            boolean result = this.removeById(config);
            if (result) {
                log.info("删除钉钉配置成功，ID: {}, 配置名称: {}", id, config.getName());
                return true;
            } else {
                log.error("删除钉钉配置失败，ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            log.error("删除钉钉配置失败，ID: {}", id, e);
            throw new RuntimeException("删除配置失败: " + e.getMessage());
        }
    }
}