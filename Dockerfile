# 第一阶段：构建
FROM maven:3.8.4-openjdk-11 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom文件并下载依赖（利用缓存）
COPY pom.xml .
# 使用阿里云镜像加速
RUN mvn dependency:go-offline -B \
    -Dmaven.test.skip=true \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true

# 复制源代码
COPY src ./src

# 打包应用（跳过测试）
RUN mvn clean package -DskipTests \
    -Dmaven.test.skip=true \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \

# 第二阶段：运行
FROM openjdk:11-jre-slim

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 创建非root用户（安全考虑）
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar包
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# 暴露端口（微信云托管使用80端口）
EXPOSE 80

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:80/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=cloud"]
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Dserver.port=80", "app.jar"]
