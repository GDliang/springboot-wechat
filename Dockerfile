# 第一阶段：构建
FROM maven:3.8.4-openjdk-11 AS builder
WORKDIR /app
COPY pom.xml .

# 使用阿里云镜像加速
RUN echo '<settings><mirrors><mirror><id>aliyun</id><name>aliyun maven</name><url>https://maven.aliyun.com/repository/public</url><mirrorOf>*</mirrorOf></mirror></mirrors></settings>' > /usr/share/maven/ref/settings.xml

RUN mvn dependency:go-offline -B -Dmaven.test.skip=true
COPY src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# 第二阶段：运行
FROM openjdk:11-jre-slim
# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone
# 安装curl（健康检查需要）
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# 创建非root用户（安全考虑）
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
WORKDIR /app

# 从构建阶段复制jar包
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# 暴露端口（使用8080，避免权限问题）
EXPOSE 8080

# ============ 优化健康检查命令 ============
# 注意：检查路径改为 /actuator/health
HEALTHCHECK --interval=30s --timeout=15s --start-period=180s --retries=8 \
  CMD curl -f --connect-timeout 5 --max-time 10 http://localhost:8080/actuator/health || exit 1

# ============ 启动命令优化 ============
# 添加JVM内存配置，移除debug参数避免日志过多
ENTRYPOINT ["java", "-jar", "-Xms256m", "-Xmx512m", "-XX:+UseG1GC", "-Dspring.profiles.active=cloud", "-Dserver.port=8080", "app.jar"]