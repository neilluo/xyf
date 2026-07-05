# Project Knowledge Base

> Auto-compiled by autopilot-evolve. Read by autopilot-analyze to reduce Spec hallucination.
> Last updated: 2026-07-06

## 已验证的技术决策

- DynamicConfigService(DB配置表) 替代 application.yml 环境变量: 热更新+集中管理 (Task: config-in-db)
- GHCR 替代阿里云ACR: 跨境ECS无法拉取大陆Region镜像 (Task: CI/CD)
- Let's Encrypt + Nginx 反代: Google OAuth 强制要求 HTTPS 域名 (Task: YouTube集成)
- RAM子用户做STS: 主账号禁止AssumeRole (Task: OSS STS配置)
- 懒初始化模式: @PostConstruct 执行时 DB 配置未加载 (Task: E2E测试)

## 必须遵守的编码规则

- Mock方法禁止进入生产: 骨架代码的mock必须在集成测试前替换 (出现2次: OAuthService + YouTubeUploader)
- 分发任务幂等性: 基于platformVideoId结果凭证而非status状态字段 (出现1次: 4份重复视频)
- Bean初始化顺序: 依赖DB配置的组件用懒初始化 (出现1次: OSS client null)
- Token自动刷新: access_token 1小时过期,必须实现refresh_token自动续期 (出现1次: 401错误)

## 已知坑点

- 阿里云RDS "Access denied 1045" 可能是白名单缺失而非密码错误
- Flyway 9.x 连MySQL 8.0需显式添加 flyway-mysql 依赖
- Alibaba Cloud Linux 4 无apt-get, 用 dnf + pip3 install certbot
- ECS元数据 /latest/meta-data/security-group-ids 在Docker容器内404
- Google OAuth redirect_uri 不接受 http://公网IP, 只接受 localhost 或 https://域名
- 阿里云安全组默认不开放 80/443, certbot HTTP验证会失败

## 项目约束（Spec 生成时必须考虑）

- 单节点架构: 新加坡ECS + RDS + OSS, 无K8s/负载均衡
- 视频流式处理: InputStream + 10MB buffer, 禁止全量加载到内存
- 配置入库: 除DB连接和TOKEN_ENCRYPT_KEY外,所有配置走system_config表
- OAuth Token加密: AES-256-GCM, 密钥从环境变量注入(鸡生蛋问题)
- YouTube隐私: 上传默认private, 需手动或API改为public
- 任务调度: DB轮询10秒 + 乐观锁拾取, 单线程池(4-8)
