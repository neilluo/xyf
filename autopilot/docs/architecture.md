# Architecture - Code Map

> 本文件由 autopilot-evolve 维护，记录项目的代码结构和模块职责。
> Last updated: 2026-07-06 (recalibrated)

## 模块总览

```
video-distributor/                        # 顶层 Maven 聚合 POM
├── distributor-server/                   # Spring Boot 服务端 (com.xyf.server)
│   └── src/main/java/com/xyf/server/
│       ├── common/                       # 通用工具
│       │   ├── constants/                # BizConstants, ErrorCode
│       │   ├── ApiKeyAuthInterceptor     # API Key 认证拦截器
│       │   ├── ApiResponse               # 统一响应格式
│       │   ├── BusinessException         # 业务异常
│       │   └── GlobalExceptionHandler    # 全局异常处理
│       ├── config/                       # Spring 配置
│       │   ├── DynamicConfigService      # DB 热配置 (system_config 表, implements ApplicationRunner)
│       │   ├── MyBatisPlusConfig         # MyBatis-Plus 配置
│       │   ├── TtlExecutorConfig         # TTL 线程池 (core=4,max=8,queue=100,traceId透传)
│       │   └── WebConfig                 # 拦截器注册
│       ├── controller/                   # REST API
│       │   ├── dto/                      # CreateTaskRequest, CreateVideoRequest
│       │   ├── AuthController            # OAuth 授权流程
│       │   ├── ConfigController          # 配置 CRUD API (group/key)
│       │   ├── TaskController            # 分发任务管理
│       │   └── VideoController           # 视频元数据管理
│       ├── domain/                       # 实体类
│       │   ├── enums/                    # AccountStatus, PlatformType, TaskStatus
│       │   ├── DistributionTask          # 分发任务实体 (含 scheduledAt, retryCount, platformVideoId)
│       │   ├── PlatformAccount           # 平台账号实体 (含 tokenExpiresAt)
│       │   ├── SystemConfig              # 系统配置实体
│       │   └── VideoMeta                 # 视频元数据实体
│       ├── log/                          # EagleEye 日志组件
│       │   ├── AccessLogFilter           # HTTP 访问日志
│       │   ├── DigestLogger              # 外部调用摘要日志 (pipe分隔)
│       │   ├── EagleEyeFilter            # traceId 注入 Filter
│       │   ├── EagleEyeIdGenerator       # 30位 traceId 生成器
│       │   └── TraceContext              # MDC + TTL 上下文
│       ├── mapper/                       # MyBatis-Plus Mapper
│       │   ├── DistributionTaskMapper    # 含 claimTask() 乐观锁方法
│       │   ├── PlatformAccountMapper
│       │   ├── SystemConfigMapper
│       │   └── VideoMetaMapper
│       ├── platform/                     # 平台适配层
│       │   ├── youtube/YouTubeUploader   # YouTube Resumable Upload + OAuth refresh
│       │   ├── tiktok/TikTokUploader     # TikTok Content Posting API v2
│       │   ├── MockUploader              # Mock 实现 (开发/测试用)
│       │   └── PlatformUploader          # 平台上传接口 (record DTOs: UploadSession/Result/TokenPair)
│       ├── scheduler/                    # 任务调度
│       │   ├── DistributionOrchestrator  # 任务执行编排器 (幂等检查→token刷新→OSS流→上传→状态更新)
│       │   ├── DistributionTaskScheduler # 10s fixedDelay DB 轮询 + ApplicationRunner(orphan recovery)
│       │   ├── GracefulShutdownManager   # 优雅停机 (SIGTERM → shutdownRequested → await)
│       │   └── RetryStrategy             # 指数退避: 30s*2^(n-1), cap=600s, maxRetry=3
│       ├── service/                      # 业务服务
│       │   ├── auth/OAuthService         # OAuth 授权 + Token 交换 (Google/TikTok)
│       │   ├── auth/TokenEncryptService  # AES-256-GCM 加解密
│       │   ├── TaskService               # 任务 CRUD + 幂等性检查 + retry/cancel
│       │   └── VideoService              # 视频元数据 CRUD
│       ├── storage/                      # OSS 存储
│       │   ├── OssStorageService         # OSS 操作封装 (懒初始化, getObjectStream 流式读取)
│       │   └── StsCredentials            # STS 临时凭证获取
│       └── DistributorServerApplication  # Spring Boot 入口
│
│   └── src/main/resources/
│       ├── application.yml               # 数据源+Flyway+Actuator配置
│       ├── logback-spring.xml            # 5文件日志隔离
│       └── db/migration/                 # Flyway SQL
│           ├── V1__init_tables.sql       # 三表初始化 (video_meta, platform_account, distribution_task)
│           ├── V2__cr_fix_indexes_and_soft_delete.sql
│           └── V3__system_config.sql     # system_config 配置表
│
├── distributor-cli/                      # picocli CLI 客户端 (com.xyf.cli)
│   └── src/main/java/com/xyf/cli/
│       ├── command/                      # 子命令
│       │   ├── ConfigCommand             # vd config (init/show/set)
│       │   ├── PublishCommand            # vd publish
│       │   ├── TaskCommand               # vd task (list/status/retry)
│       │   └── UploadCommand             # vd upload (STS + OSS直传)
│       ├── http/HttpClient               # OkHttp 封装 (Bearer Token)
│       ├── CliConfig                     # ~/.vd/config.yaml 管理
│       └── VdCommand                     # 顶层命令入口
│
├── .github/workflows/deploy.yml          # GitHub Actions → GHCR → SSH Deploy
├── deploy/docker-compose.yml             # Docker Compose 配置
└── autopilot/                            # AI Agent 知识体系
    ├── knowledge/                        # 编译知识库
    ├── hooks/                            # 质量门禁
    └── docs/                             # 架构文档
```

## 核心数据流

```
CLI upload → POST /api/v1/videos/upload-token → STS凭证 → CLI直传OSS → POST /api/v1/videos (注册元数据)
CLI publish → POST /api/v1/tasks → TaskService.createTask(幂等检查) → DB(status=PENDING, scheduledAt=now)
Scheduler(10s) → .le(scheduledAt,now) → claimTask(乐观锁) → TtlExecutor → Orchestrator.execute()
Orchestrator → 幂等检查(platformVideoId) → Token刷新(5min提前) → OSS.getObjectStream → PlatformUploader → SUCCESS/retry
启动时 → ApplicationRunner.run() → 回滚孤儿(UPLOADING/PROCESSING→PENDING)
```

## 关键接口

| 接口/组件 | 职责 | 关键行为 |
|-----------|------|----------|
| PlatformUploader | 平台上传抽象 | initUpload→uploadChunk→waitForPublish, refreshToken |
| OssStorageService | OSS操作封装 | 懒初始化, getObjectStream返回InputStream |
| DistributionTaskScheduler | DB轮询调度 | 10s poll + ApplicationRunner orphan recovery |
| DistributionOrchestrator | 任务编排 | 幂等→token→stream→upload→status |
| RetryStrategy | 指数退避 | 30s*2^(n-1), cap=600s, 通过scheduledAt延迟 |
| TokenEncryptService | AES-256-GCM | 加解密 OAuth Token |
| DynamicConfigService | DB热配置 | ApplicationRunner加载, JVM缓存 |
| TraceContext | traceId管理 | initTrace/getTraceId/clear + MDC |
| DigestLogger | 外部调用摘要 | pipe分隔日志(url|status|rt|error) |

## 技术栈版本

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.6 |
| MySQL | 8.0 |
| Flyway | managed by Spring Boot |
| 阿里云 OSS SDK | 3.18.1 |
| picocli | 4.7.6 |
| OkHttp | 4.12.0 |
| Google YouTube API | v3-rev20240514-2.0.0 |
| TTL | 2.14.5 |
