-- 创建数据库
CREATE DATABASE IF NOT EXISTS `dingtalk_alert` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `dingtalk_alert`;

-- 钉钉配置表
CREATE TABLE IF NOT EXISTS `dingtalk_config` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `webhook_url` VARCHAR(500) NOT NULL COMMENT 'Webhook地址',
    `secret` VARCHAR(200) COMMENT '密钥',
    `description` TEXT COMMENT '描述',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `create_by` VARCHAR(50) DEFAULT 'system' COMMENT '创建人',
    `update_by` VARCHAR(50) DEFAULT 'system' COMMENT '更新人',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='钉钉配置表';

-- 数据库配置表
CREATE TABLE IF NOT EXISTS `database_config` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
    `url` VARCHAR(500) NOT NULL COMMENT '数据库连接URL',
    `username` VARCHAR(100) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码',
    `driver_class_name` VARCHAR(200) NOT NULL DEFAULT 'com.mysql.cj.jdbc.Driver' COMMENT '驱动类名',
    `test_sql` VARCHAR(200) DEFAULT 'SELECT 1' COMMENT '测试SQL',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `remark` TEXT COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_name` (`config_name`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库配置表';

-- 告警模板表
CREATE TABLE IF NOT EXISTS `alert_template` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `template_content` TEXT NOT NULL COMMENT '模板内容',
    `template_fields` TEXT COMMENT '模板字段（JSON格式）',
    `template_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '模板类型：text、markdown',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `is_custom` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为自定义模板',
    `task_id` BIGINT(20) COMMENT '关联的任务ID（仅自定义模板）',
    `remark` TEXT COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_name` (`template_name`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_is_custom` (`is_custom`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警模板表';

-- 查询任务表
CREATE TABLE IF NOT EXISTS `query_task` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `sql_content` TEXT NOT NULL COMMENT 'SQL语句',
    `database_config_id` BIGINT(20) NOT NULL COMMENT '数据库配置ID',
    `alert_template_id` BIGINT(20) COMMENT '告警模板ID',
    `cron_expression` VARCHAR(100) NOT NULL COMMENT 'Cron表达式',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `status` VARCHAR(20) DEFAULT 'STOPPED' COMMENT '任务状态：RUNNING、STOPPED、PAUSED',
    `last_execute_time` DATETIME COMMENT '最后执行时间',
    `next_execute_time` DATETIME COMMENT '下次执行时间',
    `execute_count` INT(11) NOT NULL DEFAULT 0 COMMENT '执行次数',
    `success_count` INT(11) NOT NULL DEFAULT 0 COMMENT '成功次数',
    `failure_count` INT(11) NOT NULL DEFAULT 0 COMMENT '失败次数',
    `remark` TEXT COMMENT '备注',
    `alert_condition` VARCHAR(100) COMMENT '告警条件',
    `threshold` INT(11) COMMENT '告警阈值',
    `result_mode` VARCHAR(20) DEFAULT 'single_row' COMMENT 'SQL结果模式：single_row、multi_row',
    `target_field` VARCHAR(100) COMMENT '多行模式目标字段名',
    `result_format` VARCHAR(20) DEFAULT 'table' COMMENT '多行模式结果格式：table、list、json',
    `threshold_mode` VARCHAR(20) DEFAULT 'count' COMMENT '阈值模式：count、value',
    `threshold_field` VARCHAR(100) COMMENT '阈值字段名',
    `dingtalk_config_id` BIGINT(20) COMMENT '钉钉配置ID',
    `at_all` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否@所有人',
    `at_mobiles` TEXT COMMENT '@指定人员手机号',
    `custom_dingtalk_data` JSON COMMENT '自定义钉钉配置数据',
    `dingtalk_mode` VARCHAR(20) DEFAULT 'existing' COMMENT '钉钉配置模式：existing-现有配置，custom-自定义配置',
    `template_mode` VARCHAR(20) DEFAULT 'existing' COMMENT '模板模式：existing-现有模板，custom-自定义模板',
    `custom_template_data` JSON COMMENT '自定义模板配置数据',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_name` (`task_name`),
    KEY `idx_database_config_id` (`database_config_id`),
    KEY `idx_alert_template_id` (`alert_template_id`),
    KEY `idx_dingtalk_config_id` (`dingtalk_config_id`),
    KEY `idx_enabled` (`enabled`),
    KEY `idx_status` (`status`),
    KEY `idx_deleted` (`deleted`),
    KEY `idx_next_execute_time` (`next_execute_time`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询任务表';

-- 执行记录表
CREATE TABLE IF NOT EXISTS `execute_record` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `execution_id` VARCHAR(50) COMMENT '执行ID（雪花片算法生成的唯一ID）',
    `task_id` BIGINT(20) NOT NULL COMMENT '任务ID',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `status` VARCHAR(20) NOT NULL COMMENT '执行状态：SUCCESS、FAILURE、RUNNING',
    `result_count` INT(11) DEFAULT 0 COMMENT '查询结果数量',
    `result_content` LONGTEXT COMMENT '查询结果内容（JSON格式）',
    `dingtalk_status` VARCHAR(20) COMMENT '钉钉推送状态：SUCCESS、FAILURE、SKIP',
    `dingtalk_response` TEXT COMMENT '钉钉推送响应',
    `execute_duration` BIGINT(20) COMMENT '执行耗时（毫秒）',
    `error_message` TEXT COMMENT '错误信息',
    `sql_query` LONGTEXT COMMENT 'SQL查询语句',
    `logs` LONGTEXT COMMENT '执行日志',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `is_alert` TINYINT(1) DEFAULT 0 COMMENT '是否告警',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_execution_id` (`execution_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_task_status` (`task_id`, `status`),
    KEY `idx_time_range` (`start_time`, `end_time`),
    KEY `idx_is_alert` (`is_alert`),
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行记录表';

-- 插入示例数据

-- 示例数据库配置
INSERT INTO `database_config` (`config_name`, `url`, `username`, `password`, `driver_class_name`, `test_sql`, `enabled`, `remark`) VALUES
('本地MySQL', 'jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai', 'root', 'password', 'com.mysql.cj.jdbc.Driver', 'SELECT 1', 1, '本地测试数据库'),
('生产MySQL', 'jdbc:mysql://prod-db:3306/production?useUnicode=true&characterEncoding=utf8&useSSL=true&serverTimezone=Asia/Shanghai', 'prod_user', 'prod_password', 'com.mysql.cj.jdbc.Driver', 'SELECT 1', 0, '生产环境数据库');

-- 示例钉钉配置
INSERT INTO `dingtalk_config` (`name`, `webhook_url`, `secret`, `description`, `enabled`) VALUES
('默认钉钉机器人', 'https://oapi.dingtalk.com/robot/send?access_token=YOUR_ACCESS_TOKEN', 'YOUR_SECRET', '默认钉钉机器人配置', 1),
('告警钉钉机器人', 'https://oapi.dingtalk.com/robot/send?access_token=ALERT_ACCESS_TOKEN', 'ALERT_SECRET', '专用告警钉钉机器人', 1);

-- 示例告警模板
INSERT INTO `alert_template` (`template_name`, `template_content`, `template_fields`, `template_type`, `enabled`, `remark`) VALUES
('数据库监控告警', '数据库监控告警\n数据库：${database}\n表名：${table_name}\n记录数：${count}\n阈值：${threshold}\n状态：${status}', '["database", "table_name", "count", "threshold", "status"]', 'text', 1, '数据库监控告警模板'),
('系统异常告警', '## 系统异常告警\n\n**系统名称：** ${system_name}\n\n**异常类型：** ${error_type}\n\n**异常信息：** ${error_message}\n\n**发生时间：** ${occur_time}', '["system_name", "error_type", "error_message", "occur_time"]', 'markdown', 1, '系统异常告警模板');

-- 示例查询任务
INSERT INTO `query_task` (`task_name`, `sql_content`, `database_config_id`, `alert_template_id`, `dingtalk_config_id`, `cron_expression`, `enabled`, `status`, `remark`) VALUES
('用户表监控', 'SELECT "user_db" as database, "users" as table_name, COUNT(*) as count, 10000 as threshold, CASE WHEN COUNT(*) > 10000 THEN "异常" ELSE "正常" END as status FROM users', 1, 1, 1, '0 */5 * * * ?', 0, 'STOPPED', '每5分钟检查用户表记录数'),
('错误日志监控', 'SELECT "log_system" as system_name, "ERROR" as error_type, message as error_message, NOW() as occur_time FROM error_logs WHERE create_time > DATE_SUB(NOW(), INTERVAL 5 MINUTE) LIMIT 1', 1, 2, 2, '0 */5 * * * ?', 0, 'STOPPED', '每5分钟检查错误日志');

COMMIT;