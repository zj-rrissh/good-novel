# ============================================================
# 多阶段构建：Maven 编译 → JRE 运行镜像
# ============================================================

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# 1) 先复制 Maven wrapper + pom.xml → 利用 Docker 缓存依赖层
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw dependency:go-offline -B -q -s .mvn/settings.xml

COPY src/ src/
RUN ./mvnw clean package -DskipTests -B -q -s .mvn/settings.xml \
    && ls -lh target/*.jar

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="ainovel-team"
LABEL description="AI Novel Platform Backend"

# 安全：以非 root 用户运行
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /build/target/AInovel-*.jar app.jar

# 确认产物存在
RUN ls -lh /app/app.jar

# JVM 调优参数（容器感知）
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai"

# 应用配置（通过环境变量注入，覆盖 application.properties 默认值）
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=default

EXPOSE ${SERVER_PORT}

# 健康检查（依赖 Spring Actuator）
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health || exit 1

USER app

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
