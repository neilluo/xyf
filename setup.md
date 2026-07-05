# 环境配置文档 (Setup Guide)

## 基础设施清单

| 组件 | 地域 | 资源ID/地址 | 状态 |
|------|------|------------|------|
| ECS | ap-southeast-1 | `i-t4n7yn7mytoppx5r7yin` / **43.106.17.115** | ✅ 就绪，Docker+Compose已装 |
| MySQL RDS | ap-southeast-1 | `rm-t4n0j5pc1qubb12l0.mysql.singapore.rds.aliyuncs.com` | ✅ 就绪，DB已创建 |
| OSS Bucket | ap-southeast-1 | `xyf-kitchen-repo` (内网可达) | ✅ 就绪 |
| GitHub Repo | - | `neilluo/xyf` | ✅ 已创建 |
| ACR (镜像仓库) | ap-southeast-1 | 待开通 | ⏳ 待操作 |
| 域名 | - | 不使用，直接用IP | - |

---

## 1. ECS 配置

- **公网IP**: `43.106.17.115`
- **内网IP**: `172.20.64.41`
- **控制台**: https://ecs.console.aliyun.com/server/i-t4n7yn7mytoppx5r7yin/detail?regionId=ap-southeast-1
- **OS**: Alibaba Cloud Linux 4 (kernel 6.6)
- **Docker**: 24.0.9 + Compose v2.29.1
- **登录**: `ssh root@43.106.17.115`
- **用途**: 运行 video-distributor 服务 (Spring Boot Docker容器)

### ECS 上需要的目录结构
```bash
/opt/video-distributor/
├── docker-compose.yml
├── .env                    # 环境变量（密钥、DB连接等）
└── logs/                   # 日志挂载目录
```

### 安全组配置
- 443 (HTTPS) — CLI 访问
- 8080 (HTTP) — 开发调试用，生产关闭
- 22 (SSH) — 管理

---

## 2. MySQL RDS 配置

- **内网Endpoint**: `rm-t4n0j5pc1qubb12l0.mysql.singapore.rds.aliyuncs.com`
- **控制台**: https://rdsnext.console.aliyun.com/detail/rm-t4n0j5pc1qubb12l0/basicInfo?region=ap-southeast-1
- **连接方式**: 内网访问（ECS与RDS同VPC）
- **数据库用户**: `neilroot`
- **端口**: 3306
- **白名单**: 需添加 `172.20.64.41`（ECS内网IP）

### 需要创建的 Database
```sql
CREATE DATABASE video_distributor CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 内网连接字符串
```
jdbc:mysql://rm-t4n0j5pc1qubb12l0.mysql.singapore.rds.aliyuncs.com:3306/video_distributor?useSSL=true&serverTimezone=Asia/Singapore&characterEncoding=utf8mb4
```

---

## 3. OSS 配置

- **Bucket**: `xyf-kitchen-repo`
- **地域**: ap-southeast-1
- **控制台**: https://oss.console.aliyun.com/bucket/oss-ap-southeast-1/xyf-kitchen-repo/object
- **Endpoint (内网)**: `oss-ap-southeast-1-internal.aliyuncs.com`
- **Endpoint (公网)**: `oss-ap-southeast-1.aliyuncs.com`
- **Endpoint (传输加速)**: `oss-accelerate.aliyuncs.com`

### AK/SK
通过环境变量获取:
```bash
echo $ALIBABA_CLOUD_ACCESS_KEY_ID  # 通过环境变量获取，不入Git
echo $ALIBABA_CLOUD_ACCESS_KEY_SECRET  # 通过环境变量获取，不入Git
```

### OSS 目录规划
```
xyf-kitchen-repo/
├── videos/          # 原始视频文件
├── thumbnails/      # 缩略图
└── temp/            # STS上传临时目录
```

---

## 4. ACR (容器镜像仓库)

- **地域**: ap-southeast-1
- **类型**: 个人版 (免费)
- **命名空间**: `xyf-dist` (待创建)
- **仓库名**: `video-distributor` (待创建)
- **VPC内网地址**: `registry-vpc.ap-southeast-1.aliyuncs.com/xyf-dist/video-distributor`
- **公网地址**: `registry.ap-southeast-1.aliyuncs.com/xyf-dist/video-distributor`

### 操作步骤
1. 开通 ACR 个人版 (https://cr.console.aliyun.com/)
2. 在 ap-southeast-1 创建命名空间 `xyf-dist`
3. 创建仓库 `video-distributor`
4. 设置固定密码（用于CI/CD推送）

---

## 5. GitHub 配置

- **仓库**: https://github.com/neilluo/xyf
- **CI/CD**: GitHub Actions

### 需要配置的 Secrets

| Secret Name | 说明 | 值 |
|-------------|------|--------|
| `ECS_HOST` | ECS公网IP | `43.106.17.115` |
| `ECS_SSH_PRIVATE_KEY` | SSH私钥 | `.deploy_key`文件内容 |

**简化说明**：CI/CD采用直接SCP+本地构建模式，不依赖ACR镜像仓库，因此只需两个Secrets。

---

## 6. ECS .env 文件模板

部署到 `/opt/video-distributor/.env`:
```env
# 应用配置
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080

# 数据库
SPRING_DATASOURCE_URL=jdbc:mysql://<RDS内网地址>:3306/video_distributor?useSSL=true&serverTimezone=Asia/Singapore
SPRING_DATASOURCE_USERNAME=neilroot
SPRING_DATASOURCE_PASSWORD=<密码>

# OSS
OSS_ENDPOINT=oss-ap-southeast-1-internal.aliyuncs.com
OSS_BUCKET=xyf-kitchen-repo
OSS_ACCESS_KEY_ID=<AK>
OSS_ACCESS_KEY_SECRET=<SK>

# 安全
TOKEN_ENCRYPT_KEY=<随机32字符>
VD_ADMIN_API_KEY=<随机32字符>

# 日志
LOG_PATH=/app/logs
LOG_MAX_HISTORY=15
```

---

## 7. GitHub Actions Workflow 概要

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: mvn -B package -DskipTests
      - name: Docker Build & Push to ACR
        run: |
          docker login ${{ secrets.ACR_REGISTRY }} -u ${{ secrets.ACR_USERNAME }} -p ${{ secrets.ACR_PASSWORD }}
          docker build -t ${{ secrets.ACR_REGISTRY }}/${{ secrets.ACR_NAMESPACE }}/video-distributor:${{ github.sha }} .
          docker push ${{ secrets.ACR_REGISTRY }}/${{ secrets.ACR_NAMESPACE }}/video-distributor:${{ github.sha }}
      - name: Deploy to ECS
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.ECS_HOST }}
          username: root
          key: ${{ secrets.ECS_SSH_PRIVATE_KEY }}
          script: |
            docker login registry-vpc.ap-southeast-1.aliyuncs.com -u ${{ secrets.ACR_USERNAME }} -p ${{ secrets.ACR_PASSWORD }}
            docker pull registry-vpc.ap-southeast-1.aliyuncs.com/${{ secrets.ACR_NAMESPACE }}/video-distributor:${{ github.sha }}
            cd /opt/video-distributor
            docker compose down
            docker compose up -d
```

---

## 8. 待办操作清单

| # | 操作 | 方式 | 状态 |
|---|------|------|------|
| 1 | 获取ECS公网IP | Browser Use (控制台) | ⏳ |
| 2 | 获取RDS内网地址 | Browser Use (控制台) | ⏳ |
| 3 | RDS创建 video_distributor 库 | Browser Use (DMS) | ⏳ |
| 4 | 开通ACR + 创建命名空间/仓库 | Browser Use (控制台) | ⏳ |
| 5 | ECS上创建目录结构 | Computer Use (SSH) | ⏳ |
| 6 | 生成SSH密钥对 | Computer Use (本地) | ⏳ |
| 7 | GitHub配置Secrets | Browser Use (GitHub) | ⏳ |
| 8 | 推送 .github/workflows/deploy.yml | Git push | ⏳ |
