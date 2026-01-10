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

# 暴露端口（微信云托管强制要求80端口）
EXPOSE 80

# ============ 优化1：健康检查命令优化，增加重试+容错，延长启动窗口期 ============
HEALTHCHECK --interval=30s --timeout=15s --start-period=300s --retries=8 \
  CMD curl -f --connect-timeout 5 --max-time 10 http://localhost:80//actuator/health || exit 1

 # ============ 核心修复：启动命令重构（重中之重！） ============
 # 修复点1：JVM参数全部放在java -jar 后，app.jar前，保证生效
 # 修复点2：新增JVM内存配置，适配云托管容器环境，解决OOM问题（必加）
 # 修复点3：删除冗余的-Dserver.address=0.0.0.0，yml中已配置
 # 修复点4：增加端口参数-Dserver.port=80，双重保证端口生效
 # 修复点5：保留debug日志，便于排查问题
 ENTRYPOINT ["sh", "-c", "java -jar -Xms256m -Xmx512m -XX:+UseG1GC -Dspring.profiles.active=cloud -Dserver.port=80 -Ddebug=true app.jar 2>&1"]