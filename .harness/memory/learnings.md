# Project Learnings

> Append-only log. Each entry records a non-obvious discovery.
> Format: ## YYYY-MM-DD - [Topic] / **Problem**: / **Solution**: / **Lesson**:

## 2026-07-05 - [RDS连接] 
**Problem**: ECS通过内网连RDS报ERROR 1045 Access denied  
**Solution**: 在RDS控制台白名单添加ECS内网IP 172.20.64.41  
**Lesson**: 阿里云RDS的"Access denied"不一定是密码错误，白名单缺失也返回1045而非connection refused

## 2026-07-05 - [ACR跨境]
**Problem**: 北京ACR从新加坡ECS拉取镜像返回403 Forbidden  
**Solution**: 改用ghcr.io（GitHub Container Registry），全球可达且免费  
**Lesson**: 中国大陆Region的ACR不允许境外IP访问，跨境部署场景需用全球性registry

# 2026-07-05 - [AK/SK权限]
**Problem**: 环境变量中的AK/SK无法查询ECS/RDS资源信息  
**Solution**: 该AK/SK仅有OSS权限，ECS/RDS信息需通过控制台获取  
**Lesson**: RAM子账号权限最小化是好事，但需要提前了解AK/SK的权限范围

## 2026-07-06 - [OAuth Token Exchange Mock]
**Problem**: OAuthService.handleCallback() 用 "pending_exchange_" + code 作为假 token 存入 DB，导致 YouTube API 返回 401  
**Solution**: 实现真正的 POST https://oauth2.googleapis.com/token 换取 access_token + refresh_token  
**Lesson**: OAuth 流程中 authorization code → token exchange 是必须步骤，骨架代码中的 mock 必须在集成测试前替换

## 2026-07-06 - [Bean初始化顺序: @PostConstruct vs ApplicationRunner]
**Problem**: OssStorageService 的 @PostConstruct 在 DynamicConfigService(ApplicationRunner) 之前执行，导致 OSS client 为 null  
**Solution**: ensureClientInitialized() 中加入懒初始化逻辑——首次调用时重试 init()  
**Lesson**: 依赖 DB 配置的 Bean 不能在 @PostConstruct 中初始化，因为 ApplicationRunner 还没执行。用懒初始化或 @DependsOn 解决

## 2026-07-06 - [主账号禁止 AssumeRole]
**Problem**: 阿里云主账号 AK/SK 调用 STS AssumeRole 返回 "Roles may not be assumed by root accounts"  
**Solution**: 创建 RAM 子用户 video-distributor-svc，授予 AliyunSTSAssumeRoleAccess + AliyunOSSFullAccess  
**Lesson**: 阿里云安全策略禁止主账号 AssumeRole，STS 必须通过 RAM 子用户调用

## 2026-07-06 - [YouTube上传重复: 幂等性缺失]
**Problem**: 任务重试时没有检查是否已上传成功，导致同一视频在 YouTube 上出现 4 份  
**Solution**: 1) uploadChunk 成功时解析 response body 提取 video ID  2) execute() 开头检查 platformVideoId 非空则跳过  
**Lesson**: 分布式任务的幂等性保障必须基于"结果凭证"（如 platformVideoId），而非仅靠状态字段。状态可被重置，但凭证不可伪造

## 2026-07-06 - [Token自动刷新 Mock]
**Problem**: YouTubeUploader.refreshToken() 返回假 token，validateToken() 只检查非空不检查过期  
**Solution**: refreshToken() 实现真正 POST grant_type=refresh_token 到 Google；Orchestrator 改为基于 tokenExpiresAt 时间判断（提前5分钟刷新）  
**Lesson**: OAuth refresh 机制是长期运行系统的命脉，必须在首次集成测试中验证，不能留 mock

## 2026-07-06 - [Nginx + Let's Encrypt for Google OAuth]
**Problem**: Google OAuth redirect_uri 不允许 http://公网IP，必须 https://域名  
**Solution**: DNS A记录 → Nginx反代8080 → certbot签Let's Encrypt证书 → redirect_uri用 https://www.xyfkitchen.com/...  
**Lesson**: 生产级 OAuth 集成必须有域名+HTTPS，开发阶段可用 http://localhost 但部署时必须切换
