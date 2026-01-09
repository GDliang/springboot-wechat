# 使用单阶段构建
FROM openjdk:11-jre-slim

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

# 复制JAR文件
COPY target/*.jar app.jar

# 暴露端口
EXPOSE 80

# 启动应用，激活cloud profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=cloud"]