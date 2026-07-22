# ============================================
# Stage 1: Build backend with Maven
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# 先下载依赖（利用 Docker 缓存层）
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 构建项目
COPY src src
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 2: Runtime image
# ============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安装 LibreOffice（用于 DOCX 转 PDF）—— 如不需要可删除此行
RUN apk add --no-cache libreoffice

# 从 builder 复制 Spring Boot jar
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# 创建上传目录
RUN mkdir -p /app/uploads/images

VOLUME ["/app/uploads"]

ENTRYPOINT ["java", "-jar", "app.jar"]
