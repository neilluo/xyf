---
trigger: always_on
---
# Backend Rules (Java/Spring Boot)

> 基于项目实践验证的编码规则。每条规则至少出现过1次问题后沉淀。

## 命名规范
**Do**: 类名 PascalCase, 方法/变量 camelCase, 常量 UPPER_SNAKE
**Don't**: 中文拼音命名, 单字母变量(循环除外)
**Self-check**: 所有新增标识符符合Java命名规范

## ext_info 写入
**Do**: Service层创建/更新记录时，从TraceContext获取traceId写入ext_info
```java
extInfo.put("traceId", TraceContext.getTraceId());
extInfo.put("source", "api-create");
```
**Don't**: 忘记写入traceId，或在Controller层直接操作ext_info
**Self-check**: 每个INSERT/UPDATE操作的代码路径中，ext_info.traceId有赋值

## 异常处理
**Do**: 业务异常用 BusinessException(ErrorCode), 系统异常让GlobalExceptionHandler兜底
**Don't**: catch(Exception e)后吞掉异常不记录日志
**Self-check**: 没有空catch块，所有异常至少有log.error

## 流式处理
**Do**: 视频文件读取用InputStream + 固定buffer(10MB)
**Don't**: 把整个视频文件加载到byte[]或内存
**Self-check**: 代码中不存在 `new byte[fileSize]` 或 `readAllBytes()` 用于视频

## Token安全
**Do**: OAuth token存储前AES-256-GCM加密(TokenEncryptService)，从DB读取后解密
**Don't**: 明文存储token，或把解密后的token写入日志
**Self-check**: log输出中搜索不到任何token值，DB中token字段是密文

## Mock实现必须标记TODO
**Do**: 骨架代码中的mock方法用 `// TODO: replace with real implementation` 标注，并在集成测试前替换
**Don't**: 让mock方法（如返回假token、跳过真实API调用）进入生产部署
**Self-check**: 搜索代码中的 "mock"、"pending_exchange_"、"fake-" 等关键词，确保无残留

## 分发任务幂等性
**Do**: 任务执行前检查 platformVideoId 是否已存在（结果凭证），存在则跳过上传
**Don't**: 仅依赖 status 字段判断是否需要重新执行（status可被外部重置）
**Self-check**: distribution_task 表中不存在 status=SUCCESS 但 platform_video_id=NULL 的记录

## Bean初始化与配置加载顺序
**Do**: 依赖DynamicConfigService(DB配置)的Bean使用懒初始化（首次调用时init）
**Don't**: 在@PostConstruct中读取DB配置（此时ApplicationRunner可能还未执行）
**Self-check**: 新增的@PostConstruct方法不依赖DynamicConfigService的返回值

## RetryStrategy注入验证
**Do**: 独立@Component必须验证其被正确注入和调用，组件存在≠组件生效
**Don't**: 创建独立组件后不验证调用方是否正确引用
**Self-check**: 新增的@Component在至少一个调用方中被@Autowired并使用

## DynamicConfig热更新
**Do**: 运行时配置存system_config表，通过 `/api/v1/configs` 接口CRUD
**Don't**: 把运行时配置硬编码在application.yml中（需重启才能生效）
**Self-check**: 新增配置项在ConfigController中有对应API，接口字段名为group/key（非configGroup/configKey）

## 孤儿任务恢复 (ApplicationRunner)
**Do**: 调度器实现 ApplicationRunner, run() 中批量回滚 UPLOADING/PROCESSING → PENDING
```java
LambdaUpdateWrapper<DistributionTask> wrapper = new LambdaUpdateWrapper<>()
        .in(DistributionTask::getStatus, TaskStatus.UPLOADING, TaskStatus.PROCESSING)
        .set(DistributionTask::getStatus, TaskStatus.PENDING)
        .set(DistributionTask::getScheduledAt, LocalDateTime.now());
taskMapper.update(null, wrapper);
```
**Don't**: 依赖人工 SQL 修复孤儿任务, 或在 @PostConstruct 中做（此时 DB 可能未就绪）
**Self-check**: 服务重启后 distribution_task 表中不存在 UPLOADING/PROCESSING 状态记录

## scheduledAt 延迟调度
**Do**: 重试时设 scheduledAt=now()+delay, 调度器查询条件加 `.le(DistributionTask::getScheduledAt, LocalDateTime.now())`
**Don't**: 用 Thread.sleep() 实现延迟（阻塞调度线程）, 或省略 scheduledAt 条件导致提前调度
**Self-check**: RetryStrategy 中 task.setScheduledAt() 被调用, DistributionTaskScheduler 查询包含 .le(scheduledAt, now())
