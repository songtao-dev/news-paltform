# NewsFlow 架构设计文档

## 1. 项目定位

NewsFlow 是一个面向内容创作者与终端用户的分布式资讯平台。创作者侧提供素材管理、图文编辑、自动审核、定时发布一站式工作流；消费侧提供首页信息流、热度榜、全文检索、评论互动等完整用户体验；整体基于微服务架构，兼顾高可用、可扩展与可维护。

---

## 2. 整体架构

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         前端展示层 （App / H5 / Wemedia 管理后台）          │
├────────────────────────────────────────────────────────────────────────────┤
│               API Gateway  (Spring Cloud Gateway)                          │
│                  路由 · 限流 · JWT 鉴权 · 跨域                              │
├────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐  │
│  │ Article │ │  User   │ │ Comment │ │Behavior │ │ Search  │ │Wemedia  │  │
│  │ Service │ │ Service │ │ Service │ │ Service │ │ Service │ │ Service │  │
│  └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘  │
├───────┼───────────┼───────────┼───────────┼───────────┼───────────┼─────────┤
│          Kafka Cluster — 异步解耦 · 削峰填谷 · 流式计算                      │
├───────┼───────────┼───────────┼───────────┼───────────┼───────────┼─────────┤
│   Redis Cluster — 多级缓存 · 分布式锁 · 布隆过滤器 · 热点排行                 │
├───────┼───────────┼───────────┼───────────┼───────────┼───────────┼─────────┤
│  MySQL   MySQL   MongoDB   MinIO     Elasticsearch      Nacos / XXL-Job     │
│  Article  User   搜索记录   对象存储     全文索引        配置中心 / 调度       │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 技术栈

| 分类 | 技术选型 | 版本 |
|------|----------|------|
| 语言 / 基础 | Java / Spring Boot | 8 / 2.3.9.RELEASE |
| 微服务 | Spring Cloud + Spring Cloud Alibaba | Hoxton.SR10 + 2.2.5.RELEASE |
| 注册与配置 | Nacos | 1.4.1 |
| API 网关 | Spring Cloud Gateway | 2.2.8.RELEASE |
| 服务调用 | OpenFeign + HttpClient | - |
| ORM | MyBatis-Plus | 3.4.1 |
| 数据库 | MySQL | 5.1.46 驱动 |
| 缓存 | Redis (Lettuce) | 5.x |
| 消息与流计算 | Apache Kafka + Kafka Streams | 2.5.1 / 2.6.6 |
| 搜索引擎 | Elasticsearch | 7.2.0 |
| NoSQL | MongoDB | 4.x 驱动 |
| 对象存储 | MinIO | 7.1.0 |
| 任务调度 | XXL-Job | 2.2.0 |
| 内容安全 | 阿里云 Green + DFA 敏感词 + OCR | 3.4.1 SDK |
| 鉴权 | JWT (jjwt) + 双 ThreadLocal 拦截器 | 0.9.1 |
| 接口文档 | Knife4j + Swagger2 | 2.0.2 / 2.9.2 |
| 模板引擎 | Freemarker（静态页生成） | Spring Boot 内置 |
| 容器化 | Dockerfile (user / article / gateway 镜像) | - |

---

## 4. 服务模块清单

| 模块 | 端口 | 核心职责 |
|------|------|----------|
| **news-platform-gateway / app-gateway** | 51601 | App 端统一入口鉴权、路由、限流 |
| **news-platform-gateway / wemedia-gateway** | 51602 | 自媒体端统一入口鉴权、路由 |
| **news-platform-user** | 51801 | 用户注册 / 登录 / 粉丝关系 / JWT 签发 |
| **news-platform-article** | 51802 | 文章发布 / 上下架 / 详情静态化 / 热度计算 |
| **news-platform-wemedia** | 51803 | 素材中心 / 频道管理 / 审核 / 延迟发布 |
| **news-platform-search** | 51804 | ES 索引同步 / 关键词联想 / 历史记录 / 高亮搜索 |
| **news-platform-behavior** | 51805 | 关注 / 点赞 / 阅读 / 不喜欢 Kafka 消费落库 |
| **news-platform-comment** | 51806 | 评论 / 回复 / 热评 / 点赞 |
| **news-platform-schedule** | 51701 | 延迟任务引擎 / 发布时间驱动回调 |
| **news-platform-feign-api** | - | 跨服务 Feign 契约与 Fallback 定义 |
| **news-platform-model** | - | POJO / DTO / VO / 枚举 / 公共消息体 |
| **news-platform-common** | - | 统一响应 / 全局异常 / Redis 工具 / Swagger 配置 / Jackson 扩展 |
| **news-platform-utils** | - | JWT / 加解密 / SimHash / 敏感词 / 雪花 ID / 文件工具 |
| **news-platform-basic / news-file-starter** | - | MinIO 对象存储自动配置 Starter |

---

## 5. 关键设计方案

### 5.1 用户认证与鉴权

- **双端 JWT 无状态登录**：App 端与 Wemedia 端分别签发 Token，Payload 携带 userId，服务端无存储，天然支持水平扩展。
- **网关 + 拦截器双层校验**：Gateway 解析签名后透传 Header，各业务微服务 `AppTokenInterceptor / WmThreadLocalUtil` 二次校验并写入 ThreadLocal，避免网关单点压力过大。
- **角色隔离**：普通用户、自媒体用户、管理员使用不同的 Claims，接口层通过 AOP 做自定义注解权限校验。

### 5.2 自媒体文章发布链路

```
Wemedia 发布文章
  └─► DFA 本地敏感词初筛  ──不通过──► 驳回
        └─► 阿里云绿网 (文本 + 封面 OCR) 精审  ──风险──► 转人工审核状态
              └─► 审核通过
                    ├─ 立即发布：写 article 库 → Kafka 消息 → 同步 ES → 生成静态页
                    └─ 定时发布：写入 Schedule 延迟任务池 → 到点回调 Wemedia → 发布流程
```

- **延迟任务双保险**：自主实现的时间轮延迟队列（Schedule 服务）作为主执行链路；XXL-Job 每分钟扫描到期未发布任务兜底，保证发布任务零丢失。
- **ES 近实时同步**：Article 服务发布成功后发送 Kafka 消息，Search 服务监听并写入 Elasticsearch 索引；失败可通过 XXL-Job 全量重做。

### 5.3 热点文章 TopN 实时计算

```
App 行为 (Read / Like / Collect)
  └─► Kafka Topic：hot-article-stream
        └─► Kafka Streams 窗口聚合 (5 分钟 Tumbling Window)
              └─► 加权计算分数 (阅读×1 + 点赞×3 + 收藏×5 + 评论×2)
                    └─► Redis ZSet：hot:articles:{channelId}
                          └─► Article 首页接口直接 ZSet.reverseRange(0, N)
```

- 冷启动与 DB 对齐：XXL-Job 每小时批量从 MySQL 计算全量分数回填 Redis，保证最终一致。
- 布隆过滤器拦截不存在文章 ID，防止缓存穿透打到 DB。

### 5.4 详情页静态化

- 文章发布成功后异步调用 `ArticleFreemarkerService`：Freemarker 模板 + 文章数据 → HTML 字符串 → 上传 MinIO Bucket。
- App 请求详情时 Nginx/网关直接从 MinIO 拉取静态 HTML，后续接入 CDN 即可覆盖全国边缘节点。
- 上下架 / 内容变更触发覆盖重写，保证一致性。

### 5.5 评论与互动

- 评论主表 `ap_comment` 与回复表 `ap_comment_repay` 独立设计，热评采用「点赞数 + 时间衰减」复合排序。
- 所有互动事件走 Kafka 异步化：接口立即返回，消费端批量落库，用户体感 < 50ms。
- 行为幂等：对 `(userId, entryId, type)` 三元组做 DB 唯一索引 + Redis 短期去重，避免用户重复请求刷单。

### 5.6 MinIO Starter 封装

- `news-platform-basic/news-file-starter` 独立 Maven 模块，基于 `MinIOConfigProperties` 自动装配，只需在 `bootstrap.yml` 配置 endpoint / bucket / accessKey：

```yaml
file:
  minio:
    endpoint: http://minio.newsflow.internal:9000
    accessKey: ${MINIO_ACCESSKEY}
    secretKey: ${MINIO_SECRETKEY}
    bucket: newsflow-media
```

- `FileStorageService` 暴露上传、下载、删除、生成预签名 URL 四件套，业务方零配置开箱即用。

### 5.7 统一响应与全局异常

- `ResponseResult<T>` 泛型封装 + `AppHttpCodeEnum` 错误码枚举，前端按 code 分支处理。
- `@RestControllerAdvice` 的 `ExceptionCatch` 捕获业务异常、参数校验异常、兜底异常，统一格式返回，避免各服务重复 try/catch。
- `ConfusionModule` 扩展 Jackson：Long 型 ID 序列化转为 String，避免前端 JS number 精度丢失；Deserializer 支持数组兜底。

---

## 6. 性能与稳定性基线

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首页列表接口 P95 | < 100 ms | Redis 多级缓存 + DB 旁路回写 |
| 搜索接口 P95 | < 80 ms | ES + MongoDB 联想词 |
| 文章详情 P95 | < 150 ms | Freemarker 静态页 + MinIO/CDN |
| 缓存命中率 | ≥ 90 % | 热点预加载 + 布隆过滤器 |
| Kafka 消息处理 | ≥ 10k msgs/s | 分区数 × 消费者线程池 |
| 定时发布成功率 | ≥ 99.9 % | Schedule + XXL-Job 双兜底 |
| 自动审核覆盖率 | 100 % | DFA 初筛 + 云端精审双通道 |

---

## 7. 部署拓扑（参考）

- 接入层：Nginx (LB) → 2 × Gateway (app) + 2 × Gateway (wemedia)
- 业务层：user × 2, article × 3, wemedia × 2, search × 2, behavior × 2, comment × 2, schedule × 1
- 数据层：MySQL 主从, Redis Cluster (6 节点), Kafka 3 Broker, ES 3 节点, MongoDB 副本集, MinIO 4 节点
- 治理：Nacos 3 节点集群, XXL-Job Admin, Prometheus + Grafana 指标采集
