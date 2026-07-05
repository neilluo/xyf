# Video Distributor - 开发任务分解

> 基于 SPEC.md，拆解为原子级 Task。  
> 工作流：每个 Task 独立分支开发 → `mvn compile` 验证 → OCR Code Review → 合并 main

---

## Phase 1: 基础框架

### Task 1: Maven 双模块项目初始化
- **分支**: `task/01-maven-init`
- **内容**:
  - 创建父 `pom.xml`（groupId: `com.xyf`, artifactId: `video-distributor`）
  - 创建 `distributor-server` 子模块（Spring Boot 3.2.x + Java 17）
  - 创建 `distributor-cli` 子模块（picocli 4.7.x，不依赖 Spring）
  - 配置 Maven 插件：spring-boot-maven-plugin、maven-compiler-plugin
  - 确保 `mvn compile` 通过
- **验证**: `mvn compile -q` 退出码 0

### Task 2: Spring Boot 基础配置 + Flyway + 数据库 Schema
- **分支**: `task/02-springboot-flyway`
- **内容**:
  - `application.yml`：配置 datasource（从环境变量读取）、server.port=8080
  - `application-prod.yml`：生产环境配置
  - 引入 Flyway 依赖，配置 `spring.flyway.locations=classpath:db/migration`
  - 创建 `db/migration/V1__init_tables.sql`：包含 video_meta、distribution_task、platform_account 三张表的完整 DDL（含 ext_info、is_deleted 字段）
  - 确保应用能启动（H2 内存数据库用于本地测试）
- **验证**: `mvn compile -q` + `mvn test` (Spring Boot 启动测试)

### Task 3: MyBatis-Plus + 领域模型 + Mapper
- **分支**: `task/03-mybatis-domain`
- **内容**:
  - 引入 MyBatis-Plus 依赖
  - 创建实体类：`VideoMeta`、`DistributionTask`、`PlatformAccount`
  - 创建 Mapper 接口：`VideoMetaMapper`、`DistributionTaskMapper`、`PlatformAccountMapper`
  - 配置 MyBatis-Plus（驼峰映射、逻辑删除字段 is_deleted、JSON TypeHandler for ext_info/tags）
  - 编写 Mapper 单元测试（使用 H2）
- **验证**: `mvn test -pl distributor-server`

### Task 4: OSS 集成 + STS 临时凭证
- **分支**: `task/04-oss-integration`
- **内容**:
  - 引入 aliyun-sdk-oss 依赖
  - 创建 `OssStorageService`：初始化 OSSClient（从环境变量读 endpoint/bucket/ak/sk）
  - 实现 `generateStsToken()` 方法（返回临时 AccessKeyId/Secret/SecurityToken）
  - 实现 `getObjectStream(key)` 方法（内网读取 OSS 对象，返回 InputStream）
  - 创建 `OssProperties` 配置类
  - 单元测试（mock OSS Client）
- **验证**: `mvn test -pl distributor-server`

### Task 5: API Key 认证拦截器 + 统一响应格式
- **分支**: `task/05-auth-interceptor`
- **内容**:
  - 创建 `ApiKeyAuthInterceptor`：检查 `Authorization: Bearer <key>` header
  - Key 从环境变量 `VD_ADMIN_API_KEY` 读取
  - 排除 `/actuator/health` 和 OAuth 回调路径
  - 创建统一响应包装：`ApiResponse<T>`（success + data + error）
  - 创建 `GlobalExceptionHandler`（统一异常处理）
  - 编写拦截器单元测试
- **验证**: `mvn test -pl distributor-server`

### Task 6: EagleEye 日志体系
- **分支**: `task/06-eagleeye-logging`
- **内容**:
  - 创建 `EagleEyeIdGenerator`：生成30位 traceId（IP8+时间戳13+自增4+pid2+rand3）
  - 创建 `TraceContext`：MDC 管理 + rpcId 层级（pushSpan/popSpan）
  - 创建 `EagleEyeFilter`（OncePerRequestFilter）：入口自动生成 traceId 注入 MDC
  - 创建 `AccessLogFilter`：每次 HTTP 请求记录 access.log 摘要
  - 创建 `DigestLogger`：外部调用摘要（pipe 分隔格式）
  - 引入 `transmittable-thread-local` 依赖
  - 创建 `TtlExecutorConfig`：TTL 包装线程池 Bean
  - 配置 `logback-spring.xml`：5 文件隔离（application/error/access/youtube-digest/tiktok-digest）
  - 所有 Appender 用 AsyncAppender 包装
- **验证**: `mvn compile -q` + 启动应用确认日志输出格式正确

### Task 7: REST API 骨架（Video + Task 接口）
- **分支**: `task/07-rest-api`
- **内容**:
  - 创建 `VideoController`：POST /api/v1/videos, GET /api/v1/videos/{id}, GET /api/v1/videos, POST /api/v1/videos/upload-token
  - 创建 `TaskController`：POST /api/v1/tasks, GET /api/v1/tasks, GET /api/v1/tasks/{id}, POST /api/v1/tasks/{id}/retry, POST /api/v1/tasks/{id}/cancel
  - 创建对应 Service 层（VideoService、TaskService）
  - 实现分页参数支持（page/size，size 上限 100）
  - 任务创建时的幂等性检查
  - ext_info 写入 traceId
  - Spring Boot Test 接口测试
- **验证**: `mvn test -pl distributor-server`

### Task 8: CLI 基础骨架（picocli）
- **分支**: `task/08-cli-skeleton`
- **内容**:
  - `distributor-cli` 模块主类 `VdCommand`（picocli @Command）
  - 子命令骨架：ConfigCommand, UploadCommand, PublishCommand, TaskCommand, AuthCommand
  - `ConfigCommand`：实现 `vd config init`（交互式引导输入 server.url + api-key）
  - 配置文件读写：`~/.vd/config.yaml`（权限 600）
  - HTTP 客户端工具类（OkHttp + Bearer Token 自动注入）
  - 打包为可执行 fat-jar（`java -jar vd-cli.jar`）
- **验证**: `mvn package -pl distributor-cli` + `java -jar target/vd-cli.jar --help`

---

## Phase 2: 任务调度框架

### Task 9: DB 轮询调度器 + 异步执行
- **分支**: `task/09-scheduler`
- **内容**:
  - 创建 `TaskScheduler`：@Scheduled(fixedDelay=10000) 轮询 PENDING 任务
  - 使用乐观锁防重复拾取：`UPDATE SET status='UPLOADING' WHERE status='PENDING' AND id=?`
  - 提交到 TtlExecutor 异步执行
  - 创建 `PlatformUploader` 接口（platform/initUpload/uploadChunk/waitForPublish/...）
  - 创建 `MockUploader` 实现（模拟上传，sleep 5秒后返回成功）
  - 创建 `DistributionOrchestrator`：编排上传流程（token刷新→初始化→上传→等待→完成）
  - 验证完整异步流程：创建任务 → 调度器拾取 → Mock上传 → 状态更新为 SUCCESS
- **验证**: `mvn test` + 集成测试验证异步流程

### Task 10: 失败重试 + 指数退避 + 优雅停机
- **分支**: `task/10-retry-shutdown`
- **内容**:
  - 实现指数退避：`delay = min(30s × 2^(retryCount-1), 10m)`
  - 任务失败时：retry_count++，更新 scheduled_at，状态回到 PENDING
  - 超过 max_retry 时永久标记 FAILED
  - 优雅停机：监听 SIGTERM → 标记 shutdownFlag → 不再拾取新任务 → 等待当前任务完成(5min) → 退出
  - DisposableBean 注册 shutdown hook
  - 单元测试覆盖重试逻辑
- **验证**: `mvn test -pl distributor-server`

---

## Phase 3: YouTube 集成

### Task 11: YouTube OAuth 2.0 授权流程
- **分支**: `task/11-youtube-oauth`
- **内容**:
  - 创建 `AuthController`：GET /api/v1/auth/youtube/authorize-url, GET /api/v1/auth/youtube/callback, GET /api/v1/auth/youtube/status
  - 实现 OAuth 流程：生成授权URL（含 state 参数）→ 回调接收 code → 换取 token → 加密存储
  - Token 加密：AES-256-GCM（密钥从环境变量 TOKEN_ENCRYPT_KEY 读取）
  - 创建 `TokenEncryptService`
  - CLI 端 `AuthCommand`：`vd auth login --platform youtube` → 请求授权URL → 打开浏览器 → 轮询状态
  - Google API Client Library 依赖引入
- **验证**: `mvn compile -q`（OAuth 真实测试需人工）

### Task 12: YouTube Resumable Upload 实现
- **分支**: `task/12-youtube-upload`
- **内容**:
  - 创建 `YouTubeUploader implements PlatformUploader`
  - 实现 `initUpload`：调用 YouTube videos.insert API（resumable）
  - 实现 `uploadChunk`：流式分片上传（10MB chunks），使用 InputStream
  - 实现 `queryProgress`：查询已上传字节数
  - 实现 `waitForPublish`：YouTube 上传完成即为发布（无需额外等待）
  - 实现 `refreshToken`：使用 refresh_token 换取新 access_token
  - 断点续传：保存 upload_session_uri + upload_offset 到 DB
  - DigestLogger 记录每次 API 调用摘要
- **验证**: `mvn compile -q`（真实上传需 OAuth token）

### Task 13: CLI upload + publish 命令完整实现
- **分支**: `task/13-cli-upload-publish`
- **内容**:
  - `UploadCommand`：`vd upload <file> --title --description --tags --thumbnail`
    - 请求 STS token → 上传到 OSS → 注册视频元数据
    - 显示上传进度条
  - `PublishCommand`：`vd publish <video-id> --platform youtube,tiktok`
    - 创建分发任务 → 默认 --wait 模式轮询进度
    - `--latest` 支持发布最近上传的视频
    - `--no-wait` 后台执行
  - `TaskCommand`：`vd task list`, `vd task status <id>`, `vd task retry <id>`
- **验证**: `mvn package -pl distributor-cli` + `java -jar vd-cli.jar upload --help`

---

## Phase 4: TikTok 集成

### Task 14: TikTok OAuth + Content Posting API
- **分支**: `task/14-tiktok-integration`
- **内容**:
  - 创建 `TikTokUploader implements PlatformUploader`
  - OAuth 流程：TikTok Login Kit v2（authorize URL → callback → token）
  - 实现 Content Posting API v2：
    - `POST /v2/post/publish/video/init/` → 获取 upload_url
    - 分片上传（FILE_UPLOAD 模式，5-64MB chunks，按序）
    - `POST /v2/post/publish/status/fetch/` → 轮询发布状态
  - Token 刷新（24小时有效期，每次 chunk 上传前检查剩余5分钟则刷新）
  - OkHttp 封装 TikTok API Client
  - DigestLogger 记录 TikTok API 调用
- **验证**: `mvn compile -q`

---

## Phase 5: 部署与运维

### Task 15: Docker + CI/CD + 健康检查
- **分支**: `task/15-deploy-cicd`
- **内容**:
  - 完善 Dockerfile（确保与项目结构匹配）
  - 完善 docker-compose.yml
  - 完善 `.github/workflows/deploy.yml`（确保 maven 路径正确）
  - 添加 Spring Boot Actuator：`/actuator/health` 端点
  - 添加 `application-prod.yml` 生产配置
  - 确保 `mvn package` 产出的 JAR 可以 docker build 成功
  - 本地验证：`docker build -t video-distributor:test .`
- **验证**: `mvn package -DskipTests` + `docker build` 成功

---

## 执行规则

1. **每个 Task 独立分支**：从 main 切出 `task/XX-name`
2. **验证通过后**：提交 commit，push 到 GitHub
3. **OCR Code Review**：对每个 Task 的 PR 执行 `/neil-ocr-skills` 审查
4. **CR 通过后**：合并到 main
5. **下一个 Task**：从更新后的 main 切出新分支

---

## 总计

| Phase | Task 数 | 预计工时 |
|-------|---------|----------|
| Phase 1: 基础框架 | 8 | 3-4h |
| Phase 2: 任务调度 | 2 | 1-2h |
| Phase 3: YouTube | 3 | 3-4h |
| Phase 4: TikTok | 1 | 2-3h |
| Phase 5: 部署 | 1 | 1h |
| **合计** | **15** | **10-14h** |
