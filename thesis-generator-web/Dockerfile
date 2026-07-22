# ============================================
# Stage 1: Build frontend with Node
# ============================================
FROM node:20-alpine AS builder
WORKDIR /build

COPY thesis-generator-web/package.json thesis-generator-web/package-lock.json ./
RUN npm ci

COPY thesis-generator-web/ .
RUN npm run build

# ============================================
# Stage 2: Nginx 运行环境
# ============================================
FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
