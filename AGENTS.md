# Video Distributor - Agent Context

海外视频分发平台（美食博主 → YouTube/TikTok）。新加坡单节点部署。

## Tech Stack

- Java 17 / Spring Boot 3.2 / Maven
- MySQL 8.0 (Flyway迁移) / MyBatis-Plus
- 阿里云 OSS (ap-southeast-1)
- picocli (CLI模块)
- OkHttp (TikTok API) / Google YouTube SDK

## Key Commands

```bash
mvn compile -q              # 编译检查
mvn test                    # 单元测试
mvn package -DskipTests     # 打包
docker build -t video-distributor:test -f distributor-server/Dockerfile .
```

## Project Structure

```
distributor-server/    → Spring Boot 服务端（API+调度+上传）
distributor-cli/       → picocli CLI客户端
db/migration/          → Flyway SQL脚本
.harness/              → AI Agent治理体系
.github/workflows/     → CI/CD
deploy/                → 部署配置
```

## Doc Navigation

| What | Where |
|------|-------|
| 技术方案 | [SPEC.md](./SPEC.md) |
| 环境配置 | [setup.md](./setup.md) |
| Task列表 | [tasks.md](./tasks.md) |
| 代码架构 | [.harness/docs/architecture.md](.harness/docs/architecture.md) |
| 编码规则 | [.harness/rules/](.harness/rules/) |
| 经验教训 | [.harness/memory/learnings.md](.harness/memory/learnings.md) |

## Critical Rules (Top 10)

1. **ext_info 必写 traceId**：所有表的 ext_info JSON 字段必须包含当前请求的 traceId
2. **流式IO**：视频处理全程 InputStream，单 chunk 内存不超 10MB
3. **EagleEye日志**：使用 30 位 traceId + MDC 透传，业务代码零侵入
4. **Flyway管理Schema**：DDL 变更只能通过 `db/migration/VX__name.sql`，禁止手动执行
5. **API统一响应**：`{"success": true/false, "data": {}, "error": {"code":"","message":""}}`
6. **Token加密存储**：OAuth token 用 AES-256-GCM 加密，密钥从环境变量读取
7. **STS临时凭证**：CLI 上传 OSS 必须用 STS，禁止持有长期 AK/SK
8. **乐观锁防重复**：任务调度用 `UPDATE WHERE status='PENDING' AND id=?` 防并发拾取
9. **任务幂等性**：同一 (video_id, platform, account_id) 不重复创建
10. **200行上限**：本文件（AGENTS.md）绝不超过 200 行，超出自动 compaction
