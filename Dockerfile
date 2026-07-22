# ============================================
# Stage 1: Build backend with Maven (国内镜像加速)
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
# 使用阿里云 Maven 镜像加速依赖下载
COPY settings.xml /root/.m2/settings.xml
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B -q

# ============================================
# Stage 2: Runtime image
# ============================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 安装 LibreOffice（用于 DOCX 转 PDF）
RUN apk add --no-cache libreoffice

# 从 builder 复制 Spring Boot jar
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# 创建上传目录
RUN mkdir -p /app/uploads/images

VOLUME ["/app/uploads"]

ENTRYPOINT ["java", "-jar", "app.jar"]
