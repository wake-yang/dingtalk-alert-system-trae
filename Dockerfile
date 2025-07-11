# 使用官方的OpenJDK 8作为基础镜像
FROM openjdk:8-jre-alpine

# 设置维护者信息
MAINTAINER DingTalk Alert System <admin@example.com>

# 设置工作目录
WORKDIR /app

# 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 创建应用用户
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

# 复制应用JAR文件
COPY target/dingtalk-alert.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app

# 切换到应用用户
USER appuser

# 暴露端口
EXPOSE 8080

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:/app/logs/gc.log"

# 设置应用参数
ENV SPRING_PROFILES_ACTIVE=prod

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]