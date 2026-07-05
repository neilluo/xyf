# Architecture - Code Map

> 本文件由 harness 系统维护，记录项目的代码结构和模块职责。
> 首次部署后由 Evolve Mode 自动更新。

## 模块总览

```
video-distributor/
├── distributor-server/           # Spring Boot 服务端
│   └── src/main/java/com/xyf/distributor/
│       ├── domain/               # 实体类 (VideoMeta, DistributionTask, PlatformAccount)
│       ├── mapper/               # MyBatis-Plus Mapper
│       ├── service/              # 业务逻辑
│       ├── controller/           # REST API
│       ├── platform/             # 平台适配层
│       │   ├── youtube/          # YouTube Resumable Upload + OAuth
│       │   └── tiktok/           # TikTok Content Posting API v2
│       ├── scheduler/            # DB轮询任务调度
│       ├── storage/              # OSS操作 (STS/上传/下载)
│       ├── log/                  # EagleEye日志 (Filter/TraceContext/DigestLogger)
│       ├── config/               # Spring配置 (TtlExecutor/SecurityConfig)
│       └── common/               # 通用工具 (ApiResponse/BizException/TokenEncrypt)
│
├── distributor-cli/              # picocli CLI客户端
│   └── src/main/java/com/xyf/distributor/cli/
│       └── command/              # 子命令 (Config/Upload/Publish/Task/Auth)
│
├── db/migration/                 # Flyway SQL (V1__init_tables.sql ...)
├── .harness/                     # Agent治理
├── .github/workflows/            # CI/CD (deploy.yml)
└── deploy/                       # Docker配置
```

## 数据流

```
CLI upload → STS → OSS → 注册video_meta
CLI publish → 创建distribution_task → 调度器拾取 → PlatformUploader上传 → 更新状态
```

## 关键接口

| 接口 | 职责 |
|------|------|
| PlatformUploader | 平台上传抽象（YouTube/TikTok实现） |
| OssStorageService | OSS操作封装 |
| TaskScheduler | 10秒fixedDelay轮询 |
| TokenEncryptService | AES-256-GCM加解密 |
| EagleEyeFilter | HTTP入口traceId注入 |
