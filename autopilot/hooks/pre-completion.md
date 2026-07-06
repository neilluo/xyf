# Pre-Completion Checklist

Before declaring a task "done", verify:

- [ ] Code compiles without errors (`autopilot/hooks/build-gate.sh` passes)
- [ ] All existing tests still pass (`mvn test -pl distributor-server`)
- [ ] ext_info 写入 traceId（新增的 Service 操作）
- [ ] 无 mock 残留（搜索 "pending_exchange_", "fake-", "mock-video-id"）
- [ ] 视频路径无 readAllBytes()（流式处理）
- [ ] 新增配置项通过 DynamicConfigService 而非 application.yml
- [ ] 新增异步任务有 orphan recovery 策略
- [ ] Token 解密后未写入日志
- [ ] AGENTS.md 未超过 200 行
- [ ] Flyway migration 命名正确（VN__description.sql）
