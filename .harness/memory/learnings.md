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

## 2026-07-05 - [AK/SK权限]
**Problem**: 环境变量中的AK/SK无法查询ECS/RDS资源信息  
**Solution**: 该AK/SK仅有OSS权限，ECS/RDS信息需通过控制台获取  
**Lesson**: RAM子账号权限最小化是好事，但需要提前了解AK/SK的权限范围
