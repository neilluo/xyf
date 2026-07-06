# Project Knowledge Base

> Auto-compiled by autopilot-evolve. Read by autopilot-analyze to reduce Spec hallucination.
> Last updated: 2026-07-06 (recalibrated)

## 已验证的技术决策

- DynamicConfigService(DB配置表) 替代 application.yml: 热更新+集中管理, 除DB连接和TOKEN_ENCRYPT_KEY外所有配置入库 (Task: config-in-db)
- GHCR 替代阿里云ACR: 跨境ECS无法拉取大陆Region镜像, 全球可达且免费 (Task: CI/CD)
- Let's Encrypt + Nginx 反代: Google OAuth 强制要求 HTTPS 域名, redirect_uri 不接受 http://公网IP (Task: YouTube集成)
- RAM子用户做STS: 阿里云主账号禁止AssumeRole, 需RAM子用户video-distributor-svc (Task: OSS STS配置)
- 懒初始化模式: @PostConstruct 时 DB 配置未加载, 依赖 DynamicConfigService 的 Bean 须首次调用时 init (Task: Bean生命周期)
- RetryStrategy 注入 Orchestrator: failTask() 走 retryStrategy.handleFailure(), 指数退避后回 PENDING (Task: 重试机制)
- 乐观锁拾取: taskMapper.claimTask(id) 原子 UPDATE, 防并发拾取 (Task: 调度器)
- 幂等性基于结果凭证: platformVideoId 非空则跳过上传, 非依赖 status 字段 (Task: YouTube上传)
- ApplicationRunner 孤儿回滚: 启动时 UPLOADING/PROCESSING → PENDING, 解决服务重启后任务丢失 (Task: retry-strategy)
- TtlExecutor 跨线程透传: taskExecutor 用 TtlExecutors 包装, 确保 MDC(traceId) 传播到异步线程 (Task: 日志体系)

## 必须遵守的编码规则

- ext_info必写traceId: Service层创建/更新记录时从TraceContext.getTraceId()写入 (全局规则)
- Mock方法禁止进入生产: 骨架代码的mock必须在集成测试前替换 (出现2次: OAuthService + YouTubeUploader)
- 分发任务幂等性: 基于platformVideoId结果凭证而非status状态字段 (出现1次: 4份重复视频)
- Bean初始化顺序: 依赖DB配置的组件用懒初始化, @PostConstruct禁止读DynamicConfigService (出现1次: OSS client null)
- Token自动刷新: access_token 1小时过期, Orchestrator执行前检查tokenExpiresAt提前5分钟刷新 (出现1次: 401错误)
- RetryStrategy必须被注入: 独立@Component不足以生效, 必须被调用方正确@Autowired使用 (出现1次)
- 配置API字段名: /api/v1/configs 接口用 group/key 而非 configGroup/configKey (出现1次)
- scheduledAt 控制重试时机: 重试任务 status=PENDING 但 scheduledAt=未来时间, 调度器查询需 .le(scheduledAt, now()) (出现1次)

## 已知坑点

- 阿里云RDS "Access denied 1045" 可能是白名单缺失而非密码错误
- Flyway 9.x 连MySQL 8.0需显式添加 flyway-mysql 依赖
- Alibaba Cloud Linux 4 无apt-get, 用 dnf + pip3 install certbot
- Google OAuth redirect_uri 不接受 http://公网IP, 只接受 localhost 或 https://域名
- 阿里云安全组默认不开放 80/443, certbot HTTP验证会失败
- 本地开发时OSS.internal_endpoint必须清空, 否则连接VPC内网超时
- TOKEN_ENCRYPT_KEY变更后所有已加密token失效, 需重新OAuth授权
- 中国大陆Region的ACR不允许境外IP访问, 跨境需用GHCR

## 关键常量（BizConstants）

| 常量 | 值 | 含义 |
|------|-----|------|
| SCHEDULER_POLL_INTERVAL_MS | 10000 | 调度轮询间隔 10s |
| SCHEDULER_BATCH_SIZE | 10 | 每次最多拾取 10 个任务 |
| UPLOAD_CHUNK_SIZE | 10MB | 流式上传 chunk 大小 |
| PUBLISH_TIMEOUT_MS | 300000 | 等待平台处理完成 5分钟超时 |
| DEFAULT_MAX_RETRY | 3 | 最大重试次数 |
| DEFAULT_TOKEN_EXPIRE_HOURS | 1 | Token 默认过期时间 |

## 重试机制精确参数

- 公式: delay = 30s * 2^(retryCount-1), cap = 600s
- 实际序列: retry1=30s, retry2=60s, retry3=120s (第3次后永久FAILED)
- maxRetry=3, 超过后 status=FAILED + completedAt=now()
- 重试通过 scheduledAt 延迟调度: status=PENDING + scheduledAt=future

## 任务状态机

PENDING → [claimTask] → UPLOADING → [upload] → PROCESSING → [publish] → SUCCESS
   ↑                        ↓              ↓
   └── [retry/recover] ←── FAILED ←───────┘

- 启动时: UPLOADING/PROCESSING → PENDING (orphan recovery via ApplicationRunner)
- 重试: FAILED 由用户手动 retry, 或由 RetryStrategy 自动回 PENDING + scheduledAt

## 项目约束（Spec 生成时必须考虑）

- 单节点架构: 新加坡ECS(2C4G) + RDS + OSS, 无K8s/负载均衡/MQ
- 视频流式处理: InputStream + 10MB buffer(UPLOAD_CHUNK_SIZE), 禁止全量加载
- 配置入库: 除DB连接和TOKEN_ENCRYPT_KEY外, 所有配置走system_config表
- OAuth Token加密: AES-256-GCM, 密钥从环境变量注入(TOKEN_ENCRYPT_KEY)
- YouTube隐私: 上传默认private, Testing模式token 7天过期
- 线程池: core=4, max=8, queue=100, awaitTermination=300s, TTL包装
- CI/CD: GitHub Actions → Maven Build → Docker → GHCR → SSH Deploy ECS
- 日志体系: EagleEye风格, 30位traceId + MDC + TTL跨线程透传
- DB Schema: Flyway管理, 所有表含ext_info(TEXT) + is_deleted + created_at/updated_at
