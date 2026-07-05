---
trigger: always_on
---
# Backend Rules (Java/Spring Boot)

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
**Do**: 业务异常用自定义BizException, 系统异常让GlobalExceptionHandler兜底
**Don't**: catch(Exception e)后吞掉异常不记录日志
**Self-check**: 没有空catch块，所有异常至少有log.error

## 流式处理
**Do**: 视频文件读取用InputStream + 固定buffer(10MB)
**Don't**: 把整个视频文件加载到byte[]或内存
**Self-check**: 代码中不存在 `new byte[fileSize]` 或 `readAllBytes()` 用于视频

## Token安全
**Do**: OAuth token存储前AES-256-GCM加密，从DB读取后解密
**Don't**: 明文存储token，或把解密后的token写入日志
**Self-check**: log输出中搜索不到任何token值，DB中token字段是密文
