# 海外视频分发平台 - 技术方案 Spec

## 1. 项目概述

### 1.1 业务目标
为美食博主提供一个自动化视频分发服务，将视频内容一键分发到 YouTube、TikTok 等海外平台。

### 1.2 MVP 范围
- **仅服务端 + CLI 命令**，不做 Web UI
- 支持平台：YouTube、TikTok
- 核心功能：视频上传 → OSS存储 → 异步分发到目标平台
- 部署环境：阿里云新加坡单节点
- **环境配置**：详见 [setup.md](./setup.md)

### 1.3 用户故事
```
作为美食博主，我希望：
1. 通过CLI上传视频并设置标题、描述、标签
2. 一条命令将视频分发到YouTube和TikTok
3. 查看分发任务的状态和进度
4. 分发失败时自动重试，也能手动重试
5. 首次使用时通过CLI完成各平台OAuth授权
```

---

## 2. 系统架构

### 2.1 整体架构图

```
┌──────────────────────────────────────┐
│         用户本地 (国内)                │
│  ┌──────────┐                        │
│  │ CLI客户端  │ ── HTTPS ──► 新加坡ECS │
│  └──────────┘                        │
└──────────────────────────────────────┘

┌──────────────────────────────────────┐
│   阿里云 ap-southeast-1 (新加坡)      │
│                                      │
│  ┌────────────────┐  ┌──────────┐   │
│  │ ECS (2C4G)     │─►│ MySQL RDS│   │
│  │ Spring Boot    │  └──────────┘   │
│  │  • API服务      │                 │
│  │  • 任务调度     │  ┌──────────┐   │
│  │  • 视频上传     │─►│ OSS      │   │
│  └───────┬────────┘  └──────────┘   │
│          │                           │
│     ┌────┴─────┐                     │
│     ▼          ▼                     │
│  ┌──────┐  ┌──────┐                 │
│  │YTube │  │TikTok│                 │
│  └──────┘  └──────┘                 │
└──────────────────────────────────────┘
```

**核心简化**：个人使用，全部部署在新加坡单节点。无需CRR、无需Master-Worker分离、无需跨域通信。

### 2.2 核心设计决策

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 部署地域 | ap-southeast-1 (新加坡) | 海外节点直达YouTube/TikTok，网络稳定 |
| 架构风格 | 单节点单进程 | 个人使用，极简架构，避免过度设计 |
| 任务队列 | DB轮询 + 进程内异步执行 | 无中间件依赖，单节点内完成 |
| CLI框架 | picocli | Java生态最成熟的CLI框架 |
| 构建工具 | Maven | 稳定可靠，AI Coding工具链兼容性好 |
| API认证 | API Key (Bearer Token) | 单用户场景足够 |
| OSS上传 | STS临时凭证 | CLI不持有长期AK/SK |
| Token加密 | AES-256-GCM，环境变量注入 | 密钥不入库 |
| DB迁移 | Flyway | SQL版本控制，应用启动自动执行 |
| 日志体系 | 阿里EagleEye风格 (neil-logging-skill) | 30位traceId + MDC透传 + 5文件隔离 |
| 扩展字段 | 每表ext_info JSON（含traceId） | 全链路可追溯 + 灵活扩展 |
| CI/CD | GitHub Actions (neil-cicd-skills) | ACR镜像 + SSH部署新加坡ECS |

### 2.3 模块划分

```
video-distributor/
├── distributor-server/         # 服务端（单节点，API+调度+上传全在一起）
│   ├── domain/                 # 领域模型
│   ├── service/                # 业务服务
│   ├── platform/               # 平台适配层
│   │   ├── youtube/            # YouTube实现
│   │   └── tiktok/             # TikTok实现
│   ├── scheduler/              # 任务调度
│   ├── controller/             # REST API
│   ├── storage/                # OSS存储服务
│   ├── log/                    # EagleEye日志组件
│   │   ├── EagleEyeIdGenerator
│   │   ├── TraceContext
│   │   ├── EagleEyeFilter
│   │   ├── AccessLogFilter
│   │   └── DigestLogger
│   └── config/                 # 配置（TtlExecutor等）
└── distributor-cli/            # CLI客户端（独立可执行JAR）
    └── command/                # picocli命令定义
```

**单模块server**：不再区分Master/Worker，API服务、任务调度、视频上传全在同一进程内完成。

---

## 3. 技术栈

| 层次 | 技术选型 | 版本 |
|------|----------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.0 |
| DB迁移 | Flyway | 10.x |
| 对象存储 | 阿里云 OSS | SDK 3.18.x |
| CLI框架 | picocli | 4.7.x |
| HTTP客户端 | OkHttp | 4.12.x |
| YouTube SDK | google-api-services-youtube | v3 |
| JSON | Jackson | 2.17.x |
| 构建 | Maven | 3.9.x |
| 部署 | Docker + 阿里云ECS | - |
| Native编译 | GraalVM | 23.x (CLI原生构建) |

---

## 4. 数据库设计

### 4.1 ER图

```
video_meta (1) ──→ (N) distribution_task
platform_account (1) ──→ (N) distribution_task
```

### 4.2 表结构

#### video_meta - 视频元数据
```sql
CREATE TABLE video_meta (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 1 COMMENT '用户ID（预留多用户扩展）',
    title VARCHAR(256) NOT NULL COMMENT '视频标题',
    description TEXT COMMENT '视频描述',
    tags JSON COMMENT '标签列表',
    category VARCHAR(64) COMMENT '分类',
    oss_bucket VARCHAR(64) NOT NULL,
    oss_key VARCHAR(512) NOT NULL COMMENT 'OSS对象Key',
    oss_region VARCHAR(32) NOT NULL DEFAULT 'ap-southeast-1',
    file_size BIGINT NOT NULL COMMENT '文件大小(bytes)',
    file_format VARCHAR(16) COMMENT '文件格式(mp4/mov等)',
    duration_seconds INT COMMENT '视频时长(秒)',
    resolution VARCHAR(16) COMMENT '分辨率(如1920x1080)',
    thumbnail_oss_key VARCHAR(512) COMMENT '缩略图OSS Key',
    ext_info JSON COMMENT '扩展参数，含traceId用于链路追溯',
    is_deleted TINYINT UNSIGNED DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频元数据';
```

**ext_info 标准结构**：
```json
{
  "traceId": "0a1b2c3d4e1719821234560001ab123",
  "source": "cli-upload",
  "clientIp": "1.2.3.4"
}
```

#### distribution_task - 分发任务
```sql
CREATE TABLE distribution_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 1 COMMENT '用户ID（预留）',
    video_id BIGINT NOT NULL COMMENT '关联video_meta.id',
    platform VARCHAR(32) NOT NULL COMMENT '目标平台: YOUTUBE|TIKTOK',
    account_id BIGINT NOT NULL COMMENT '关联platform_account.id',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|UPLOADING|PROCESSING|SUCCESS|FAILED',
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    scheduled_at DATETIME NOT NULL COMMENT '计划执行时间',
    started_at DATETIME,
    completed_at DATETIME,
    error_message TEXT COMMENT '最近一次错误信息',
    platform_video_id VARCHAR(256) COMMENT '平台返回的视频ID/URL',
    upload_session_uri TEXT COMMENT '断点续传会话URI',
    upload_offset BIGINT DEFAULT 0 COMMENT '已上传字节数',
    platform_metadata JSON COMMENT '平台特有的元数据覆盖',
    ext_info JSON COMMENT '扩展参数，含traceId用于链路追溯',
    is_deleted TINYINT UNSIGNED DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_schedule (status, scheduled_at),
    INDEX idx_video (video_id),
    INDEX idx_platform_status (platform, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分发任务';
```

#### platform_account - 平台账号
```sql
CREATE TABLE platform_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 1 COMMENT '用户ID（预留）',
    platform VARCHAR(32) NOT NULL COMMENT 'YOUTUBE|TIKTOK',
    account_name VARCHAR(128) NOT NULL COMMENT '账号标识/名称',
    access_token TEXT COMMENT '访问令牌(加密存储)',
    refresh_token TEXT COMMENT '刷新令牌(加密存储)',
    token_expires_at DATETIME COMMENT 'access_token过期时间',
    channel_id VARCHAR(128) COMMENT 'YouTube频道ID / TikTok open_id',
    extra_config JSON COMMENT '平台特有配置',
    ext_info JSON COMMENT '扩展参数，含traceId用于链路追溯',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|EXPIRED|REVOKED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_platform_account (platform, account_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台账号配置';
```

---

## 5. CLI 命令设计

### 5.1 命令总览

```bash
vd <command> [options]

Commands:
  auth      平台账号授权管理
  upload    上传视频到OSS
  publish   发布视频到目标平台
  task      查看/管理分发任务
  config    配置管理
```

### 5.2 详细命令

#### 授权管理
```bash
# YouTube OAuth授权（打开浏览器完成授权）
vd auth login --platform youtube --account my-channel

# TikTok OAuth授权
vd auth login --platform tiktok --account my-tiktok

# 查看已授权账号
vd auth list

# 检查token状态
vd auth status --platform youtube

# 撤销授权
vd auth revoke --platform youtube --account my-channel
```

#### 视频上传
```bash
# 上传视频到OSS（自动获取STS临时凭证，支持断点续传）
vd upload ./cooking-video.mp4 \
  --title "红烧肉家常做法" \
  --description "今天教大家做一道经典红烧肉..." \
  --tags "美食,红烧肉,家常菜" \
  --thumbnail ./thumbnail.jpg

# 上传并立即分发
vd upload ./video.mp4 --title "..." --publish youtube,tiktok
```

**上传流程**：
1. CLI向服务端请求STS临时凭证（`POST /api/v1/videos/upload-token`）
2. 使用STS凭证直传视频到OSS（传输加速端点）
3. 上传完成后向服务端注册视频元数据（`POST /api/v1/videos`）
4. STS凭证有效1小时，仅限`oss:PutObject`到指定前缀路径

#### 视频发布
```bash
# 将已上传的视频分发到指定平台（默认--wait，显示实时进度）
vd publish <video-id> --platform youtube,tiktok

# 发布最近上传的视频
vd publish --latest --platform youtube,tiktok

# 后台分发（不等待结果）
vd publish <video-id> --platform youtube --no-wait

# 只发布到YouTube，指定隐私级别
vd publish <video-id> --platform youtube --privacy public

# 定时发布
vd publish <video-id> --platform youtube --schedule "2024-01-15T20:00:00+08:00"
```

**`--schedule`语义**：利用YouTube自身的定时发布能力（设置`publishAt`参数），视频立即上传但设为private，到达指定时间后自动转为public。TikTok不支持API层面的定时发布，该参数仅对YouTube生效。

```bash
# 覆盖特定平台的元数据
vd publish <video-id> --platform tiktok --title "TikTok专用标题"
```

**默认行为**：`vd publish`默认带`--wait`，提交任务后自动进入轮询模式，以进度条形式展示分发状态：
```
⚡ 分发到 YouTube... [===========         ] 55% | 上传中 (128MB/232MB)
✅ YouTube 分发完成! https://youtube.com/watch?v=xxx
⚡ 分发到 TikTok...  [====                ] 20% | 上传中
```

#### 任务管理
```bash
# 查看所有任务
vd task list

# 查看指定视频的分发状态
vd task status <video-id>

# 重试失败的任务
vd task retry <task-id>

# 重试所有失败任务
vd task retry-all

# 取消任务
vd task cancel <task-id>
```

#### 配置管理
```bash
# 初始化配置（交互式引导，逐步提示）
vd config init
# → 请输入服务端地址: https://vd.yourdomain.com
# → 请输入API Key: xxxxxxxx
# → ✅ 配置完成！连接测试成功。

# 查看当前配置
vd config show

# 手动设置服务端地址
vd config set server.url https://vd.yourdomain.com
```

**配置文件位置**：`~/.vd/config.yaml`，权限600，内容包含：
- `server.url` — 服务端地址
- `server.api-key` — API认证密钥

**API Key生成机制**：服务端首次部署时通过环境变量`VD_ADMIN_API_KEY`预设置，或启动时自动生成并输出到日志（仅首次）。

---

## 6. 核心接口设计

### 6.1 REST API（CLI → 主控服务）

**认证方式**：所有API请求需携带 `Authorization: Bearer <api-key>`，首次 `vd config init` 时生成随机API Key并存储在服务端配置中。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/videos | 注册视频元数据 |
| GET | /api/v1/videos/{id} | 获取视频信息 |
| GET | /api/v1/videos | 列出所有视频 |
| POST | /api/v1/videos/upload-token | 获取OSS STS临时上传凭证 |
| POST | /api/v1/tasks | 创建分发任务 |
| GET | /api/v1/tasks | 列出分发任务 |
| GET | /api/v1/tasks/{id} | 获取任务详情 |
| POST | /api/v1/tasks/{id}/retry | 重试任务 |
| POST | /api/v1/tasks/{id}/cancel | 取消任务 |
| GET | /api/v1/auth/{platform}/authorize-url | 获取OAuth授权URL |
| GET | /api/v1/auth/{platform}/callback | OAuth回调（浏览器重定向） |
| GET | /api/v1/auth/{platform}/status?state=xx | CLI轮询授权完成状态 |
| GET | /api/v1/auth/accounts | 列出授权账号 |

**分页参数**：列表接口支持 `?page=1&size=20`，size上限100，默认按`created_at DESC`排序。

**统一响应格式**：
成功：`{"success": true, "data": {...}}`
失败：`{"success": false, "error": {"code": "TOKEN_EXPIRED", "message": "..."}}`

### 6.2 平台适配接口

```java
public interface PlatformUploader {
    String platform();
    UploadSession initUpload(VideoMeta video, PlatformAccount account);
    /** 流式上传，避免大文件加载到内存 */
    UploadResult uploadChunk(UploadSession session, InputStream chunkStream, long offset, long chunkSize, long totalSize);
    long queryProgress(UploadSession session);
    /** 等待平台异步处理完成（TikTok需要轮询发布状态） */
    PublishResult waitForPublish(UploadSession session, Duration timeout);
    void setThumbnail(String platformVideoId, InputStream thumbnail, PlatformAccount account);
    TokenPair refreshToken(String refreshToken);
    boolean validateToken(String accessToken);
}
```

**内存安全设计**：全程使用InputStream流式处理，从本地OSS流式读取并直接pipe到平台API，单个chunk内存占用不超过10MB。

---

## 7. 关键流程

### 7.1 视频分发主流程

```
CLI: vd publish <video-id> --platform youtube,tiktok
  │
  ├─→ POST /api/v1/tasks (创建2个分发任务，状态=PENDING)
  │
  ▼ [任务调度器] (10秒轮询DB，进程内异步执行。使用 `fixedDelay`（上一次完成后再等10秒），通过乐观锁（`UPDATE SET status='UPLOADING' WHERE status='PENDING' AND id=?`）防止重复拾取)
  │
  ├─→ 执行上传
  │     ├─ 1. 从本地OSS读取视频（内网）
  │     ├─ 2. 刷新平台Token（如需要）
  │     ├─ 3. 初始化上传会话
  │     ├─ 4. 流式分片上传视频
  │     ├─ 5. 等待平台处理完成
  │     ├─ 6. 上传缩略图
  │     └─ 7. 更新任务状态 SUCCESS/FAILED
  │
  └─→ CLI: vd task status <video-id> 查询进度
```

### 7.2 OAuth授权流程

```
CLI: vd auth login --platform youtube
  │
  ├─→ 1. CLI向主控服务请求授权URL（含随机state参数）
  │      GET /api/v1/auth/youtube/authorize-url?state=abc123
  ├─→ 2. 主控服务生成授权URL（redirect_uri指向主控公网地址）
  ├─→ 3. CLI打开浏览器访问授权URL
  ├─→ 4. 用户在浏览器中完成授权
  ├─→ 5. Google/TikTok回调主控服务
  │      GET /api/v1/auth/youtube/callback?code=xxx&state=abc123
  ├─→ 6. 主控服务用code换取token，加密存储
  ├─→ 7. CLI轮询授权状态（每2秒）
  │      GET /api/v1/auth/youtube/status?state=abc123
  └─→ 8. 收到成功响应，显示授权完成
```

**关键点**：
- redirect_uri 注册为主控服务公网地址（如 `http://43.106.17.115:8080/api/v1/auth/youtube/callback`）
- CLI不启动本地HTTP Server，通过轮询确认授权结果
- state参数防CSRF，同时作为CLI关联授权结果的凭据

### 7.3 失败重试流程

```
任务状态 = FAILED, retry_count < max_retry
  │
  ├─→ 计算下次重试时间（指数退避: `delay = min(30s × 2^(retryCount-1), 10m)`，即 30s, 2m, 10m）
  ├─→ 更新 scheduled_at = 下次重试时间
  ├─→ 更新 status = PENDING
  │
  ▼ [调度器下次轮询到达 scheduled_at]
  │
  ├─→ 检查是否有断点续传信息
  │     ├─ 有 → 从断点继续
  │     └─ 无/会话过期 → 重新初始化上传
  │
  └─→ 执行上传...
```

**特殊情况**：
- Token过期：尝试自动refresh，若refresh_token也失效则标记FAILED并提示"需重新授权"

### 7.4 Token刷新策略

| 平台 | access_token有效期 | 刷新策略 |
|--------|------------------|----------|
| YouTube | 1小时 | Resumable Upload session URI独立于token，不需过程中刷新 |
| TikTok | 24小时 | 每次chunk上传前检查剩余5分钟则自动刷新 |

**并发刷新保护**：同一账号token刷新时加DB行锁（`SELECT ... FOR UPDATE`），避免多任务并发刷新导致旧refresh_token失效。

### 7.5 任务幂等性

创建分发任务时检查：
- 已存在相同 `(video_id, platform, account_id)` 且状态为 SUCCESS → 返回已有的platform_video_id
- 已存在相同组合且状态为 PENDING/UPLOADING/PROCESSING → 返回已有任务ID
- 不存在或只有FAILED状态 → 创建新任务

---

## 8. 部署方案

### 8.1 新加坡单节点

| 配置项 | 值 |
|--------|-----|
| 地域 | ap-southeast-1 (新加坡) |
| 机型 | ecs.t6-c1m2.large (2C4G) |
| OS | Alibaba Cloud Linux 4 |
| 数据库 | RDS MySQL 8.0 基础版 (1C1G) |
| OSS Bucket | video-dist-sg (ap-southeast-1) |
| 功能 | API服务 + 任务调度 + 视频上传（全在一个进程） |

### 8.2 网络配置

- ECS开启HTTPS（Let's Encrypt证书）
- OSS开启传输加速（可选，仅当国内上传速度不理想时开启）
- 安全组：仅开放 443(HTTPS) + 22(SSH管理)
- MySQL仅内网访问

### 8.3 成本估算（月）

| 资源 | 费用(估) | 备注 |
|------|----------|------|
| ECS ap-southeast-1 (2C4G) | ¥150 | 年付折扣 |
| RDS MySQL (1C1G) | ¥80 | 年付折扣 |
| OSS 存储 (100GB) | ¥12 | |
| ECS出网流量 (50GB/月) | ¥150 | 主要成本项 |
| 其他（域名/磁盘/IP） | ¥30 | |
| **合计** | **~¥420/月** | |

### 8.4 运维与监控

#### 健康检查
- Spring Boot Actuator `/actuator/health`
- Docker `HEALTHCHECK` + restart=always
- 阿里云云监控基础告警

#### 日志方案（neil-logging-skill）
- **5个日志文件**：application.log / error.log / access.log / youtube-digest.log / tiktok-digest.log
- **行格式**：`%d %-5level [%X{traceId},%X{rpcId}] [%thread] %logger{36} - %msg%n`
- **轮转**：Logback按天+100MB，保畇15天
- **digest行**：pipe分隔，每次外部调用(YouTube/TikTok API)记录一行摘要
- **集中收集**：阿里云SLS（免费额度500MB/天）

#### CI/CD 流程（neil-cicd-skills）
```
Git Push → GitHub Actions → Maven Build + Test → Docker Build → Push to ACR
                                                                  │
                                                                  ▼
                                                    SSH Deploy 新加坡 ECS
                                                    (docker compose up -d)
```
- 镜像仓库：阿里云ACR（个人版免费）
- 回滚策略：保留前5个镜像版本，回滚=切换tag并重启

#### 优雅停机
1. 收到SIGTERM后停止接收新任务
2. 等待当前上传任务完成（最长5分钟）
3. 超时强制退出，依赖断点续传恢复
4. Docker `stop_grace_period: 5m`

---

## 9. AI Coding Harness 工程体系

### 9.1 设计原则
- **知识库驱动**：通过AGENTS.md和Coding Rules为AI提供完整的项目上下文
- **幻觉防控**：严格的质量门禁 + 编译验证 + 测试覆盖
- **经验沉淀**：踩坑记录自动写入memory，避免重复犯错

### 9.2 文件结构

```
video-distributor/
├── AGENTS.md                    # AI Agent上下文入口
├── .qoder/
│   └── rules/
│       ├── architecture.md      # 架构规则
│       ├── coding-style.md      # 代码风格规则
│       ├── database.md          # 数据库规范
│       ├── error-handling.md    # 异常处理规范
│       └── security.md          # 安全规范
├── docs/
│   ├── api-spec.md              # API规范
│   └── platform-notes.md       # 平台接入注意事项
└── scripts/
    ├── start.sh                 # 启动脚本
    └── stop.sh                  # 停止脚本
```

### 9.3 AGENTS.md 核心内容要点

```markdown
# Video Distributor - AI Agent Context

## Project Overview
海外视频分发平台，将美食视频分发到YouTube/TikTok。

## Architecture
- 单节点Spring Boot应用，部署在新加坡
- API服务 + 任务调度 + 视频上传 全在同一进程
- DB轮询实现任务队列，无需MQ中间件

## Key Constraints
1. YouTube API必须用OAuth2.0，不支持Service Account
2. TikTok使用Content Posting API v2 (`https://open.tiktokapis.com/v2/post/publish/`)
   - 上传模式: FILE_UPLOAD (分片推送)
   - 鉴权scope: `video.publish` + `video.upload`
   - 官方文档: https://developers.tiktok.com/doc/content-posting-api-get-started
3. TikTok无官方Java SDK，用OkHttp封装
4. 通过内网访问OSS，不走公网
5. Token加密存储（AES-256-GCM）
6. 全程流式IO，单chunk内存不超10MB

## Module Guide
[各模块职责和依赖关系]

## How to Build & Run
[构建和运行命令]
```

### 9.4 质量门禁

| 阶段 | 检查项 | 工具 |
|------|--------|------|
| 编码时 | 编译通过 | Maven Compiler |
| 编码时 | 代码规范 | Checkstyle (Google Style) |
| 提交前 | 单元测试通过 | JUnit 5 + Mockito |
| 提交前 | 静态分析 | SpotBugs |
| 集成时 | 接口测试 | Spring Boot Test |
| 部署前 | Docker构建成功 | Dockerfile |

### 9.5 AI Coding 工作流

```
1. AI读取AGENTS.md了解项目全貌
2. AI读取相关Coding Rules了解约束
3. AI实现代码
4. 运行 mvn compile（编译门禁）
5. 运行 mvn test（测试门禁）
6. 如有失败 → 自动修复 → 回到步骤4
7. 通过 → 提交
8. 记录经验到memory（如有踩坑）
```

---

## 10. MVP 里程碑规划

### Phase 1: 基础框架（预计AI 2-3小时）
- [ ] Maven双模块项目初始化 (server + cli)
- [ ] Spring Boot配置
- [ ] Flyway + 数据库Schema
- [ ] MyBatis-Plus配置
- [ ] OSS集成 + STS临时凭证
- [ ] 基本CLI骨架(picocli)
- [ ] API Key认证拦截器
- [ ] EagleEye日志体系 (neil-logging-skill)

### Phase 2: 任务调度框架（预计AI 2-3小时）
- [ ] DB轮询调度器 + 异步执行
- [ ] MockUploader验证异步流程
- [ ] 失败重试 + 指数退避
- [ ] 任务幂等性检查
- [ ] 优雅停机

### Phase 3: YouTube集成（预计AI 3-4小时）
- [ ] OAuth2.0授权流程（服务端回调模式）
- [ ] YouTube视频上传（Resumable Upload，流式）
- [ ] Token自动刷新策略
- [ ] 缩略图上传
- [ ] 断点续传

### Phase 4: TikTok集成（预计AI 2-3小时）
- [ ] OAuth2.0授权流程
- [ ] TikTok Content Posting API v2 (FILE_UPLOAD模式)
- [ ] 分片上传实现
- [ ] Publish状态轮询

### Phase 5: 部署与运维（预计AI 2-3小时）
- [ ] Dockerfile
- [ ] docker-compose配置
- [ ] GitHub Actions CI/CD
- [ ] 环境变量管理
- [ ] 健康检查 + Actuator
- [ ] 日志配置(Logback JSON + SLS)

### Phase 6: 优化（可选/延后）
- [ ] GraalVM Native Image构建CLI
- [ ] 多用户支持
- [ ] Web UI

---

## 11. 安全设计

### 11.1 认证与授权

| 层次 | 方案 | 说明 |
|------|------|------|
| CLI → 服务端 | API Key (Bearer Token) | 初始化时生成随机32字符密钥 |
| CLI → OSS | STS临时凭证 | 有效1小时，限制PutObject指定前缀 |

### 11.2 密钥管理

- **OAuth Token加密**：AES-256-GCM，密钥通过`TOKEN_ENCRYPT_KEY`环境变量注入
- **禁止密钥入库**：`.gitignore`包含所有密钥文件，CI检查无硬编码密钥
- **后续演进**：可接入阿里云KMS管理加密密钥轮转

### 11.3 网络安全

- Master API必须启用HTTPS（Let's Encrypt证书）
- API服务仅通过8080端口对外暴露，Actuator管理端点仅绑定localhost。安全组限制仅开放443(HTTPS)+22(SSH)。
- 数据库仅内网访问，不开放公网

### 11.4 数据安全

- OAuth refresh_token 加密存储，日志中脱敏
- OSS Bucket禁止公开读取，所有访问需签名
- CLI本地配置文件权限设为600（仅所有者可读写）

---

## 12. 风险和待确认事项

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| YouTube OAuth审核周期长 | 正式发布前token 7天过期 | 先用Testing模式开发，申请Production并行 |
| TikTok应用审核周期不确定 | 未审核只能发私密视频 | 先实现功能，审核通过后切换为公开 |
| 跨境网络不稳定 | 上传可能超时/中断 | 断点续传 + 重试机制 |
| 海外ECS被限流 | 多任务并发时触发平台Rate Limit | 并发控制 + 令牌桶限流 |
