# 钉钉告警系统

一个基于Spring Boot的钉钉告警系统，支持通过自定义SQL查询和钉钉模板定时推送告警信息。

## 功能特性

- 🔧 **多数据源管理**：支持配置多个数据库连接，动态管理数据源
- 📝 **自定义SQL查询**：支持编写自定义SQL语句进行数据查询
- 📱 **钉钉消息推送**：支持文本、Markdown等多种消息格式
- ⏰ **定时任务调度**：基于Quartz实现灵活的定时任务调度
- 🎨 **模板化消息**：支持自定义消息模板，动态替换变量
- 📊 **执行记录管理**：详细记录任务执行历史和结果
- 🌐 **Web管理界面**：提供友好的Web界面进行配置管理
- 🐳 **Docker支持**：提供完整的Docker部署方案

## 技术栈

- **后端框架**：Spring Boot 2.7.18
- **数据库**：MySQL 8.0 + MyBatis Plus
- **连接池**：Druid
- **定时任务**：Quartz
- **前端模板**：Thymeleaf
- **工具库**：Hutool、FastJSON
- **容器化**：Docker + Docker Compose

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 8.0+
- Docker（可选）

### 本地开发

1. **克隆项目**
```bash
git clone <repository-url>
cd dingtalk-alert
```

2. **配置数据库**
```bash
# 创建数据库
mysql -u root -p < src/main/resources/sql/init.sql
```

3. **修改配置**
```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dingtalk_alert
    username: your_username
    password: your_password
```

4. **编译运行**
```bash
mvn clean package
java -jar target/dingtalk-alert.jar
```

5. **访问系统**
```
http://localhost:8080
```

### Docker部署

1. **使用Docker Compose**
```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f dingtalk-alert-app

# 停止服务
docker-compose down
```

2. **单独构建镜像**
```bash
# 构建应用镜像
mvn clean package
docker build -t dingtalk-alert:latest .

# 运行容器
docker run -d \
  --name dingtalk-alert \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://your-db:3306/dingtalk_alert \
  -e SPRING_DATASOURCE_USERNAME=your_username \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  dingtalk-alert:latest
```

## 使用指南

### 1. 配置数据库连接

在「数据库配置」页面添加需要监控的数据库连接：

- 配置名称：便于识别的名称
- 连接URL：数据库连接地址
- 用户名/密码：数据库认证信息
- 测试SQL：用于测试连接的SQL语句

### 2. 创建告警模板

在「告警模板」页面创建钉钉消息模板：

- 模板名称：便于识别的名称
- 模板内容：支持变量替换的消息内容
- 模板类型：text（文本）或 markdown
- Webhook地址：钉钉机器人的Webhook地址
- 密钥：钉钉机器人的加签密钥

**文本模板示例：**
```
数据库监控告警
数据库：${database}
表名：${table_name}
记录数：${count}
阈值：${threshold}
状态：${status}
```

**Markdown模板示例：**
```markdown
## 数据库监控告警

**数据库：** ${database}

**表名：** ${table_name}

**记录数：** ${count}

**阈值：** ${threshold}

**状态：** ${status}
```

### 3. 创建查询任务

在「查询任务」页面创建定时查询任务：

- 任务名称：便于识别的名称
- SQL语句：查询语句，结果字段将作为模板变量
- 数据库配置：选择要查询的数据库
- 告警模板：选择消息模板
- Cron表达式：定时执行规则

**SQL示例：**
```sql
SELECT 
  'user_database' as database,
  'users' as table_name,
  COUNT(*) as count,
  10000 as threshold,
  CASE WHEN COUNT(*) > 10000 THEN '异常' ELSE '正常' END as status
FROM users
WHERE create_time > DATE_SUB(NOW(), INTERVAL 1 HOUR)
```

**Cron表达式示例：**
- `0 */5 * * * ?` - 每5分钟执行
- `0 0 9 * * ?` - 每天上午9点执行
- `0 0 9 * * MON-FRI` - 工作日上午9点执行

### 4. 钉钉机器人配置

1. 在钉钉群中添加自定义机器人
2. 选择「加签」安全设置
3. 复制Webhook地址和密钥
4. 在系统中配置相应信息

## API接口

系统提供RESTful API接口，主要包括：

### 数据库配置
- `GET /api/database/list` - 分页查询数据库配置
- `POST /api/database/save` - 保存数据库配置
- `POST /api/database/test` - 测试数据库连接

### 告警模板
- `GET /api/template/list` - 分页查询告警模板
- `POST /api/template/save` - 保存告警模板
- `POST /api/template/test` - 测试模板消息

### 查询任务
- `GET /api/task/list` - 分页查询查询任务
- `POST /api/task/save` - 保存查询任务
- `POST /api/task/execute/{id}` - 立即执行任务
- `POST /api/task/toggle/{id}` - 启用/禁用任务

### 执行记录
- `GET /api/record/list` - 分页查询执行记录
- `GET /api/record/statistics` - 获取执行统计

### 钉钉配置
- `POST /api/dingtalk/test-connection` - 测试钉钉连接
- `POST /api/dingtalk/send-test-message` - 发送测试消息

## 监控端点

系统集成了Spring Boot Actuator，提供以下监控端点：

- `/actuator/health` - 健康检查
- `/actuator/info` - 应用信息
- `/actuator/metrics` - 性能指标
- `/druid` - Druid监控面板（用户名：admin，密码：admin123）

## 配置说明

### 应用配置

主要配置项说明：

```yaml
# 服务端口
server:
  port: 8080
  servlet:
    context-path: /dingtalk-alert

# 数据源配置
spring:
  datasource:
    druid:
      # 连接池配置
      initial-size: 5
      min-idle: 5
      max-active: 20
      
# 自定义配置
dingtalk:
  webhook:
    timeout: 10000
    retry-count: 3
    
scheduler:
  thread-pool-size: 10
  auto-startup: true
```

### 环境变量

Docker部署时可使用以下环境变量：

- `SPRING_PROFILES_ACTIVE` - 激活的配置文件
- `SPRING_DATASOURCE_URL` - 数据库连接URL
- `SPRING_DATASOURCE_USERNAME` - 数据库用户名
- `SPRING_DATASOURCE_PASSWORD` - 数据库密码

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查数据库服务是否启动
   - 验证连接参数是否正确
   - 确认网络连通性

2. **钉钉消息发送失败**
   - 验证Webhook地址格式
   - 检查密钥配置
   - 确认机器人权限设置

3. **定时任务不执行**
   - 检查Cron表达式格式
   - 确认任务是否启用
   - 查看应用日志

### 日志查看

```bash
# Docker环境
docker-compose logs -f dingtalk-alert-app

# 本地环境
tail -f logs/dingtalk-alert.log
```

## 开发指南

### 项目结构

```
src/main/java/com/dingtalk/alert/
├── controller/          # 控制器层
├── service/            # 服务层
├── mapper/             # 数据访问层
├── entity/             # 实体类
├── config/             # 配置类
└── DingTalkAlertApplication.java  # 启动类

src/main/resources/
├── application.yml     # 主配置文件
├── application-docker.yml  # Docker环境配置
├── sql/               # 数据库脚本
└── templates/         # 页面模板
```

### 扩展开发

1. **添加新的消息类型**
   - 在`DingTalkService`中添加新的发送方法
   - 更新模板类型枚举
   - 修改前端选择组件

2. **支持新的数据库类型**
   - 添加对应的JDBC驱动依赖
   - 更新`DatabaseService`中的驱动类名映射
   - 调整SQL兼容性

3. **自定义调度策略**
   - 继承`SchedulerService`
   - 实现自定义的任务调度逻辑
   - 注册为Spring Bean

## 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 贡献

欢迎提交Issue和Pull Request来改进项目。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 邮箱：admin@example.com
- 项目地址：<repository-url>

---

**注意**：使用前请确保已正确配置钉钉机器人，并妥善保管相关密钥信息。