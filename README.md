<div align="center">


# 📰 新视界新闻头条（NewSight News Platform）

**基于 Spring Cloud 微服务架构的资讯平台：文章发布 + 内容审核 + 智能搜索 + 实时热点 + 用户行为分析**

[![Java](https://img.shields.io/badge/Java-1.8-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.9-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR10-brightgreen)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/SCA-2.2.5.RELEASE-blue)](https://github.com/alibaba/spring-cloud-alibaba)
[![Nacos](https://img.shields.io/badge/Nacos-注册配置中心-blue)](https://nacos.io/)
[![Kafka Streams](https://img.shields.io/badge/Kafka-Streams-231F20)](https://kafka.apache.org/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-7.2.0-yellow)](https://www.elastic.co/)
[![Redis](https://img.shields.io/badge/Redis-缓存-red)](https://redis.io/)
[![FastDFS](https://img.shields.io/badge/FastDFS-对象存储-C72E49)](https://github.com/happyfish100/fastdfs)
[![XXL-Job](https://img.shields.io/badge/XXL--Job-分布式任务-green)](https://www.xuxueli.com/xxl-job/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](#许可证)

</div>

---

## 📖 项目简介

新视界新闻头条是一个**面向 C 端用户的资讯平台微服务系统**，完整覆盖文章发布、内容审核、智能搜索、实时热点排行、用户行为分析、评论互动等业务链路。系统基于 Spring Cloud + Spring Cloud Alibaba 微服务架构，拆分为 7 个业务微服务 + 双网关，通过 Nacos 实现服务注册与配置中心，Kafka 实现异步解耦与流式计算，Elasticsearch 提供全文检索能力。

> 💡 **设计初衷**：传统单体资讯平台在高并发阅读场景下存在 DB 行锁竞争、发布审核阻塞主流程、MySQL LIKE 搜索性能差等问题。本项目通过微服务拆分 + Kafka 异步解耦 + Kafka Streams 流式聚合 + 多级缓存 + 文章静态化，实现详情页 RT 10ms、首页 RT 5ms、搜索 RT < 200ms 的高性能体验。

### 解决的核心问题

| 痛点                                          | 解决方案                                    | 效果                         |
| --------------------------------------------- | ------------------------------------------- | ---------------------------- |
| 文章详情 3 表 JOIN + 模板渲染，RT 200ms+      | Freemarker 静态化 + FastDFS 存储 HTML       | 详情 RT → **10ms**           |
| 用户点赞/阅读直接 UPDATE DB，热门文章行锁竞争 | Kafka Streams 10s 窗口聚合后批量落库        | DB 写入 QPS 降 **10-100 倍** |
| 首页按时间倒序全表扫描，DB QPS 数千           | XXL-Job 全量热点 + Redis 多频道 Top 30 缓存 | 首页 RT → **5ms**            |
| 文章发布同步审核（文本+图片 2-3s）阻塞        | 延迟队列 + @Async 异步审核                  | 发布 RT → **100ms**          |
| MySQL LIKE 搜索全表扫描，不分词               | ES IK 分词 + BoolQuery + 高亮               | 搜索 RT → **< 200ms**        |
| 单网关路由混乱，自媒体流量影响 App 用户       | 双网关独立部署（App 端 / 自媒体端）         | 流量与权限域隔离             |
| 任务调度内存 Timer，重启丢失 + 无法分布式     | MySQL 双表持久化 + Redis 分级缓存延迟队列   | 任务 0 丢失，多实例可扩展    |

---

## 🏗️ 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                客户端层                                      │
│           App 端（用户读文章）              自媒体端（作者发文）               │
└──────────────────┬───────────────────────────────────┬──────────────────────┘
                   │                                   │
                   ▼                                   ▼
┌──────────────────────────────┐  ┌──────────────────────────────┐
│  App 网关 (51601)             │  │  自媒体网关 (51602)           │
│  Spring Cloud Gateway         │  │  Spring Cloud Gateway         │
│  GlobalFilter JWT 鉴权         │  │  GlobalFilter JWT 鉴权         │
│  注入 userId → 请求头          │  │  注入 wmUserId → 请求头        │
└──────────────┬───────────────┘  └──────────────┬───────────────┘
               │                                   │
               └───────────────────┬───────────────┘
                                   │
                   ┌───────────────▼───────────────┐
                   │    Nacos 注册中心 + 配置中心    │
                   └───────────────┬───────────────┘
                                   │
    ┌──────────┬───────────┬───────┴───────┬──────────┬──────────┬──────────┐
    ▼          ▼           ▼               ▼          ▼          ▼          ▼
┌────────┐ ┌────────┐ ┌────────┐     ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│article │ │  user  │ │wemedia │     │behavior│ │ search │ │schedule│ │comment │
│ 文章服务│ │ 用户服务│ │ 自媒体 │     │ 行为服务│ │ 搜索服务│ │ 调度服务│ │ 评论服务│
│        │ │        │ │        │     │        │ │        │ │        │ │        │
│ 静态化  │ │ 注册登录│ │ 发文审核│     │ 阅读点赞│ │ ES搜索 │ │ 延迟队列│ │ 评论互动│
│ 热点计算│ │ JWT签发│ │ 频道管理│     │ 收藏关注│ │ 联想词  │ │ 任务调度│ │        │
│ Kafka  │ │        │ │ 素材管理│     │        │ │ 搜索记录│ │        │ │        │
│ Streams│ │        │ │        │     │        │ │        │ │        │ │        │
└───┬────┘ └────────┘ └───┬────┘     └───┬────┘ └───┬────┘ └───┬────┘ └────────┘
    │                      │              │          │          │
    │    ┌─────────────────┘              │          │          │
    │    │   Feign 接口契约模块(feign-api) │          │          │
    │    └───────────────────────────────────────────┘          │
    │                                                           │
    ▼          ▼          ▼          ▼          ▼               ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐   ┌──────────┐
│ MySQL  │ │ Redis  │ │ Kafka  │ │   ES   │ │FastDFS │   │ XXL-Job  │
│ (15表) │ │(缓存/锁)│ │(异步/流)│ │(全文检索)│ │(静态HTML)│   │(定时任务) │
└────────┘ └────────┘ └────────┘ └────────┘ └────────┘   └──────────┘
```

### 核心数据流：文章发布全链路

```
作者提交文章
    │
    ▼
自媒体网关鉴权 → wemedia 服务
    │
    ├── ① 保存 wm_news（状态=提交 4）
    ├── ② WmNewsTaskService.addNewsToTask() → Feign 调用 schedule 服务
    │       └── MySQL taskinfo + taskinfo_logs 双表持久化
    │       └── Redis ZSet（未来5分钟内）/ List（立即执行）分级缓存
    │       └── Protostuff 二进制序列化任务参数
    │
    ▼  到点后 wemedia 每秒 poll 拉取任务
    │
    ├── ③ WmNewsAutoScanService.autoScan() @Async 异步审核
    │       ├── 文本审核：阿里云 Green antispam（反垃圾）
    │       ├── 图片审核：阿里云 Green porn（鉴黄）+ terrorism（暴恐）
    │       ├── 敏感词过滤：SensitiveWordUtil（DFA 算法）
    │       ├── 内容去重：SimHash 相似度计算
    │       └── 三态决策：pass（自动发布）/ review（人工复审）/ block（拒绝）
    │
    ▼  审核通过
    │
    ├── ④ Feign 调用 article 服务保存文章（ap_article + content + config 3 表）
    │       └── @Async ArticleFreemarkerService.buildArticleToMinIO()
    │           ├── Freemarker 模板渲染 HTML（article.ftl）
    │           ├── 上传 FastDFS → 回写 static_url
    │           └── Kafka 投递 ES 同步消息（article_es_sync_topic）
    │
    ▼
    ├── ⑤ search 服务 SyncArticleListener @KafkaListener 消费 → 写入 ES 索引
    │
    ▼
    └── ⑥ 用户可在 App 端搜索 / 阅读 / 点赞 / 评论
            └── 行为发 Kafka（hot_article_score_topic）
                └── Kafka Streams 10s 窗口按 articleId 聚合
                    └── 投递 hot_article_incr_handle_topic
                        └── ArticleIncrHandleListener 增量更新 MySQL + Redis Top 30
```

---

## ✨ 核心特性

### 1. 双网关 JWT 鉴权 + 用户上下文透传

- App 端网关（51601）与自媒体端网关（51602）独立部署，流量与权限域完全隔离
- 每个网关实现 `GlobalFilter` + `Ordered` 统一 JWT 校验（JJWT HS256），登录路径（含 `/login`）放行
- 校验通过后从 Claims 提取 `id`，注入请求头 `userId` / `wmUserId`，下游微服务零信任直连
- 下游 `HandlerInterceptor`（AppTokenInterceptor / WmTokenInterceptor）解析请求头存入 `ThreadLocalUtil`
- `afterCompletion` 清理 ThreadLocal，规避 Tomcat 线程池复用导致的用户身份串号

### 2. 文章静态化三段式异步流水线

- `@Async` 串联 Freemarker 模板渲染 → FastDFS 上传 HTML → Kafka 投递 ES 同步消息
- 文章详情访问从"3 表 JOIN + 模板渲染"降级为"FastDFS 直返 HTML"，可由 CDN 缓存
- 详情页 RT 从 200ms 降至 **10ms**，DB QPS 从 3000+ 降至 0（详情不走 DB）
- 文章上下架通过 Kafka 消息（article_is_down_topic）异步同步 ES 索引状态

### 3. Kafka Streams 实时热点计算

- 用户行为（阅读/点赞/评论/收藏）发 Kafka 消息 `hot_article_score_topic`
- `TimeWindows.of(Duration.ofSeconds(10))` 滑动窗口按 articleId 分组聚合
- 聚合器维护 `COLLECTION:0,COMMENT:0,LIKES:0,VIEWS:0` 四维度计数，状态存储 `hot-atricle-stream-count-001`
- 聚合结果投递 `hot_article_incr_handle_topic`，`ArticleIncrHandleListener` 增量更新 MySQL 计数 + Redis 热点
- DB 写入 QPS 从 5000+ 降至 50-500，行锁竞争消除，热点实时性从分钟级降至 **10 秒级**

### 4. XXL-Job 全量热点 + Redis 多频道缓存

- `@XxlJob("computeHotArticleJob")` 每天定时全量重算最近 50 天文章热点
- 热点评分公式：`score = likes*3 + views + comment*5 + collection*8`
- 按频道维度各缓存 Top 30 到 Redis（key: `HOT_ARTICLE_${channelId}`）
- 首页 `firstPage=true` 优先读 Redis，命中直接返回，RT 从 100ms 降至 **5ms**
- Redis miss 时 fallback 到 DB 查询，全量重算保证数据新鲜度

### 5. 自研分布式延迟队列（MySQL + Redis 双层）

- 任务持久化到 `taskinfo`（活跃任务，执行后删除保持表小）+ `taskinfo_logs`（全量审计，version 乐观锁）双表
- 按执行时间分级缓存：立即执行 → Redis List（`TOPIC_${type}_${priority}`），未来 5 分钟内 → Redis ZSet（`FUTURE_${type}_${priority}`），超过 5 分钟 → 仅入库
- `@Scheduled(fixedRate = 60000)` 每分钟扫描 ZSet，Redis Pipeline 批量 `LPUSH + ZREM`（1 次 RTT 完成）
- `tryLock`（SET NX EX 30）实现多实例抢锁，支持水平扩展
- Protostuff 二进制序列化任务参数，体积比 JSON 减少 **60%+**
- 提供 `addTask` / `pollTask` / `cancelTask` 三大核心接口

### 6. Elasticsearch 智能搜索

- `BoolQueryBuilder` 分离 `must`（title + content 多字段分词评分）与 `filter`（publishTime 范围缓存），性能提升 5-10 倍
- `HighlightBuilder` 标题关键词高亮回显，按发布时间倒序分页
- ES 索引同步走 Kafka 异步解耦（`article_es_sync_topic`），文章发布主流程零阻塞
- 联想词功能：`ApAssociateWords` 表维护关联词，输入时实时联想
- 异步保存用户搜索记录到 `ap_user_search`，支持搜索历史

### 7. 阿里云内容安全审核 + 多重内容校验

- 集成阿里云 Green SDK：文本 `antispam`（反垃圾）+ 图片 `porn`（鉴黄）+ `terrorism`（暴恐）
- 三态决策机：`pass`（通过自动发布）/ `review`（转人工复审）/ `block`（拒绝）
- 审核耗时 1-2s 通过 `@Async` 异步化，发布接口 RT 从 2-3s 降至 **100ms**
- 额外内容校验：SensitiveWordUtil（DFA 敏感词过滤）+ SimHashUtils（内容去重）

### 8. Feign 接口契约下沉 + 微服务零信任

- 独立 `news-platform-feign-api` 模块集中管理所有跨服务调用接口（只含 interface + DTO，无业务依赖）
- 提供方 `@RestController implements IClient` 编译期校验签名一致性
- 消费方零业务依赖，避免 MyBatis/JPA 等无关依赖传递污染
- 下游微服务不做 JWT 校验，完全信任网关注入的 userId 请求头（网关层统一鉴权）

---

## 🛠️ 技术栈

| 类别                 | 技术                       | 版本           | 用途                              |
| -------------------- | -------------------------- | -------------- | --------------------------------- |
| **语言**             | Java                       | 1.8            | 后端开发                          |
| **微服务框架**       | Spring Cloud               | Hoxton.SR10    | 微服务治理                        |
| **Web 框架**         | Spring Boot                | 2.3.9.RELEASE  | 应用基础框架                      |
| **微服务套件**       | Spring Cloud Alibaba       | 2.2.5.RELEASE  | Nacos / Sentinel 集成             |
| **网关**             | Spring Cloud Gateway       | -              | 双网关路由 + 鉴权                 |
| **注册/配置中心**    | Nacos                      | 2.x            | 服务注册发现 + 配置管理           |
| **服务调用**         | OpenFeign                  | -              | 声明式 HTTP 客户端                |
| **消息队列**         | Kafka                      | 2.6.6          | 异步解耦 + 事件驱动               |
| **流计算**           | Kafka Streams              | 2.5.1          | 用户行为窗口聚合                  |
| **数据库**           | MySQL                      | 5.7+           | 关系型数据持久化（15 张表）       |
| **ORM**              | MyBatis-Plus               | 3.4.1          | 数据访问层（Lambda 类型安全查询） |
| **缓存**             | Redis                      | 6.x+           | 热点缓存 + 分布式锁 + 延迟队列    |
| **搜索引擎**         | Elasticsearch              | 7.2.0          | 文章全文检索 + 高亮 + 联想词      |
| **对象存储**         | FastDFS                    | -              | 静态 HTML + 图片存储              |
| **定时任务**         | XXL-Job                    | 2.2.0          | 分布式定时任务调度                |
| **模板引擎**         | Freemarker                 | -              | 文章静态化 HTML 渲染              |
| **序列化**           | Protostuff                 | -              | 延迟任务参数二进制序列化          |
| **认证**             | JJWT                       | 0.9.1          | JWT 生成与解析（HS256）           |
| **密码加密**         | BCrypt                     | -              | 密码哈希（自研实现）              |
| **内容审核**         | 阿里云 Green SDK           | 3.4.1          | 文本反垃圾 + 图片鉴黄暴           |
| **分词**             | HanLP                      | portable-1.3.4 | 中文分词                          |
| **内容去重**         | SimHash                    | -              | 文章相似度计算去重                |
| **ZooKeeper 客户端** | Curator                    | 4.2.0          | 分布式协调                        |
| **API 文档**         | Knife4j + Swagger          | 2.0.2 / 2.9.2  | 接口文档 + 在线调试               |
| **日志**             | Log4j2                     | -              | 日志框架（排除默认 Logback）      |
| **JSON**             | Fastjson                   | 1.2.58         | JSON 序列化                       |
| **工具库**           | Commons-Lang3 / Commons-IO | -              | 通用工具方法                      |

---

## 📁 项目结构

```
news-platform/
├── news-platform-common/                 # 公共模块
│   └── com.reynasky.common
│       ├── aliyun/                       # 阿里云内容审核
│       │   ├── GreenImageScan.java       # 图片鉴黄暴
│       │   ├── GreenTextScan.java        # 文本反垃圾
│       │   └── util/                     # 自定义图库上传
│       ├── constants/                    # 常量定义
│       │   ├── ArticleConstants.java
│       │   ├── HotArticleConstants.java  # Kafka Topic 常量
│       │   ├── ScheduleConstants.java
│       │   ├── WemediaConstants.java
│       │   └── WmNewsMessageConstants.java
│       ├── exception/                    # 全局异常处理
│       │   ├── CustomException.java
│       │   └── ExceptionCatch.java
│       ├── jackson/                      # Jackson 混淆序列化（敏感数据保护）
│       │   ├── ConfusionDeserializer.java
│       │   ├── ConfusionSerializer.java
│       │   └── InitJacksonConfig.java
│       ├── redis/                        # Redis 缓存服务封装
│       │   └── CacheService.java         # 50+ 方法封装（ZSet/List/Pipeline/锁）
│       └── swagger/                      # Swagger 配置
│
├── news-platform-utils/                  # 工具模块
│   └── com.reynasky.utils
│       ├── common/
│       │   ├── AppJwtUtil.java           # JWT 生成解析
│       │   ├── BCrypt.java               # 密码哈希
│       │   ├── ProtostuffUtil.java       # 二进制序列化
│       │   ├── SimHashUtils.java         # 内容去重
│       │   ├── SensitiveWordUtil.java    # DFA 敏感词过滤
│       │   ├── SnowflakeIdWorker.java    # 雪花 ID 生成
│       │   ├── BurstUtils.java           # 分屏工具
│       │   ├── Compute.java              # 热点计算
│       │   ├── DESUtils.java / MD5Utils.java / Base64Utils.java
│       │   ├── DateUtils.java / FileUtils.java / ZipUtils.java
│       │   ├── IdsUtils.java / ReflectUtils.java / UrlSignUtils.java
│       │   └── JdkSerializeUtil.java
│       └── thread/
│           ├── AppThreadLocalUtil.java   # App 用户上下文
│           └── WmThreadLocalUtil.java    # 自媒体用户上下文
│
├── news-platform-model/                  # 数据模型模块
│   ├── article/                          # 文章实体（ApArticle / Content / Config）
│   ├── user/                             # 用户实体（ApUser / ApUserFan / ApUserFollow）
│   ├── wemedia/                          # 自媒体实体（WmNews / WmUser / WmChannel / WmMaterial）
│   ├── behavior/                         # 行为实体（点赞/阅读/不喜欢/关注/收藏）
│   ├── search/                           # 搜索实体（搜索记录/联想词）
│   ├── schedule/                         # 调度实体（Taskinfo / TaskinfoLogs）
│   ├── comment/                          # 评论实体
│   ├── dtos/                             # 请求 DTO
│   ├── vos/                              # 响应 VO
│   └── mess/                             # Kafka 消息对象（UpdateArticleMess / ArticleVisitStreamMess）
│
├── news-platform-feign-api/              # Feign 接口契约模块
│   ├── ArticleClient.java                # 文章服务接口
│   ├── WemediaClient.java                # 自媒体服务接口
│   ├── ScheduleClient.java               # 调度服务接口
│   └── UserClient.java                   # 用户服务接口
│
├── news-platform-gateway/                # 网关层
│   ├── news-platform-app-gateway/        # App 端网关（端口 51601）
│   │   ├── filter/AuthorizeFilter.java   # JWT 鉴权 GlobalFilter
│   │   ├── util/AppJwtUtil.java
│   │   └── config/CorsConfig.java
│   └── news-platform-wemedia-gateway/    # 自媒体端网关（端口 51602）
│       └── filter/AuthorizeFilter.java
│
├── news-platform-service/                # 业务微服务层
│   ├── news-platform-article/            # 文章服务（端口 51802）
│   │   ├── controller/v1/
│   │   │   ├── ArticleHomeController.java    # 首页/加载更多
│   │   │   ├── ArticleInfoController.java    # 文章详情
│   │   │   └── ApCollectionController.java   # 收藏
│   │   ├── service/
│   │   │   ├── ApArticleService.java
│   │   │   ├── ArticleFreemarkerService.java # 静态化流水线
│   │   │   ├── HotArticleService.java        # 热点计算
│   │   │   └── ApCollectionService.java
│   │   ├── stream/HotArticleStreamHandler.java    # Kafka Streams 聚合
│   │   ├── listener/
│   │   │   ├── ArticleIncrHandleListener.java     # 聚合结果消费
│   │   │   └── ArticleIsDownListener.java         # 上下架同步
│   │   ├── job/ComputeHotArticleJob.java          # XXL-Job 全量热点
│   │   ├── interceptor/AppTokenInterceptor.java
│   │   └── config/KafkaStreamConfig / XxlJobConfig
│   │
│   ├── news-platform-user/               # 用户服务
│   │   └── controller/v1/UserController.java
│   │
│   ├── news-platform-wemedia/            # 自媒体服务
│   │   ├── controller/v1/
│   │   │   ├── LoginController.java
│   │   │   ├── WmNewsController.java     # 文章发布
│   │   │   ├── WmchannelController.java  # 频道管理
│   │   │   └── WmMaterialController.java # 素材管理
│   │   ├── service/
│   │   │   ├── WmNewsAutoScanService.java    # 内容审核（文本+图片+敏感词+去重）
│   │   │   ├── WmNewsTaskService.java        # 延迟任务调度
│   │   │   └── WmUserService.java
│   │   └── interceptor/WmTokenInterceptor.java
│   │
│   ├── news-platform-behavior/           # 行为服务
│   │   ├── controller/v1/
│   │   │   ├── ApReadBehaviorController.java
│   │   │   ├── ApLikesBehaviorController.java
│   │   │   ├── ApUnlikesBehaviorController.java
│   │   │   └── (关注行为)
│   │   └── service/ (阅读/点赞/不喜欢/关注/收藏)
│   │
│   ├── news-platform-search/             # 搜索服务
│   │   ├── controller/v1/
│   │   │   ├── ArticleSearchController.java   # ES 文章搜索
│   │   │   ├── ApUserSearchController.java    # 搜索记录
│   │   │   └── ApAssociateWordsController.java # 联想词
│   │   ├── service/ArticleSearchService.java  # BoolQuery + 高亮
│   │   ├── listener/SyncArticleListener.java  # Kafka ES 同步
│   │   └── config/ElasticSearchConfig.java
│   │
│   ├── news-platform-schedule/           # 调度服务（延迟队列）
│   │   ├── service/TaskService.java      # addTask/poll/cancel
│   │   ├── mapper/TaskinfoMapper + TaskinfoLogsMapper
│   │   └── feign/ScheduleClient.java
│   │
│   └── news-platform-comment/            # 评论服务
│
├── news-platform-basic/                  # 基础依赖聚合模块
├── docs/                                 # 文档
├── pom.xml                               # 父 POM（依赖管理）
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- **JDK** 1.8+
- **Maven** 3.6+
- **MySQL** 5.7+
- **Redis** 6.x+
- **Kafka** 2.5+（含 ZooKeeper）
- **Elasticsearch** 7.2.0（IK 分词插件）
- **FastDFS**（Tracker + Storage）
- **Nacos** 2.x（注册 + 配置中心）
- **XXL-Job** 2.2.0（分布式定时任务调度中心）

### 1. 启动中间件

```bash
# 建议使用 Docker Compose 一键启动 MySQL / Redis / Kafka / Elasticsearch / Nacos / XXL-Job / FastDFS
docker compose up -d
```

### 2. 初始化数据库

```bash
# 导入建表脚本（按业务模块分表）
mysql -u root -p < docs/sql/init.sql
```

### 3. 配置 Nacos

在 Nacos 控制台（默认 `http://localhost:8848/nacos`）创建各微服务的配置：

| Data ID                                  | 说明                                          |
| ---------------------------------------- | --------------------------------------------- |
| `news-platform-app-gateway-prod.yml`     | App 网关路由规则                              |
| `news-platform-wemedia-gateway-prod.yml` | 自媒体网关路由规则                            |
| `news-platform-article-prod.yml`         | 文章服务（DB/Redis/Kafka/FastDFS/ES/XXL-Job） |
| `news-platform-user-prod.yml`            | 用户服务                                      |
| `news-platform-wemedia-prod.yml`         | 自媒体服务（阿里云审核密钥）                  |
| `news-platform-behavior-prod.yml`        | 行为服务（Kafka）                             |
| `news-platform-search-prod.yml`          | 搜索服务（ES 地址）                           |
| `news-platform-schedule-prod.yml`        | 调度服务（DB/Redis）                          |

敏感配置（数据库密码、阿里云 Key、FastDFS 密钥）通过 Nacos 加密配置或环境变量注入，不硬编码到代码。

### 4. 编译 & 启动

```bash
# 编译全部模块
mvn clean install -DskipTests

# 按依赖顺序启动各服务
# 1. Nacos（已启动则跳过）
# 2. 双网关
cd news-platform-gateway/news-platform-app-gateway && mvn spring-boot:run
cd news-platform-gateway/news-platform-wemedia-gateway && mvn spring-boot:run
# 3. 业务服务（顺序无强制依赖，Feign 懒加载）
cd news-platform-service/news-platform-user && mvn spring-boot:run
cd news-platform-service/news-platform-article && mvn spring-boot:run
cd news-platform-service/news-platform-wemedia && mvn spring-boot:run
cd news-platform-service/news-platform-behavior && mvn spring-boot:run
cd news-platform-service/news-platform-search && mvn spring-boot:run
cd news-platform-service/news-platform-schedule && mvn spring-boot:run
cd news-platform-service/news-platform-comment && mvn spring-boot:run
```

### 5. 验证

| 服务                | 地址                                  |
| ------------------- | ------------------------------------- |
| App 端网关          | `http://localhost:51601`              |
| 自媒体端网关        | `http://localhost:51602`              |
| 文章服务            | `http://localhost:51802`              |
| Nacos 控制台        | `http://localhost:8848/nacos`         |
| XXL-Job 控制台      | `http://localhost:8080/xxl-job-admin` |
| Elasticsearch       | `http://localhost:9200`               |
| API 文档（Knife4j） | `http://localhost:51802/doc.html`     |

---

## ⚙️ 核心配置说明

| 配置项                                     | 说明                 | 默认值                                           |
| ------------------------------------------ | -------------------- | ------------------------------------------------ |
| `server.port`                              | 服务端口             | App网关 51601 / 自媒体网关 51602 / article 51802 |
| `spring.cloud.nacos.discovery.server-addr` | Nacos 注册中心地址   | localhost:8848                                   |
| `spring.cloud.nacos.config.server-addr`    | Nacos 配置中心地址   | localhost:8848                                   |
| `spring.cloud.nacos.config.file-extension` | 配置文件格式         | yml                                              |
| `spring.profiles.active`                   | 环境                 | dev / prod                                       |
| `spring.kafka.bootstrap-servers`           | Kafka 地址           | localhost:9092                                   |
| `spring.elasticsearch.rest.uris`           | ES 地址              | localhost:9200                                   |
| `fdfs.tracker-list`                        | FastDFS Tracker 地址 | localhost:22122                                  |
| `aliyun.green.access-key-id`               | 阿里云审核 Key       | -                                                |
| `aliyun.green.secret`                      | 阿里云审核 Secret    | -                                                |
| `xxl.job.admin.addresses`                  | XXL-Job 调度中心     | localhost:8080/xxl-job-admin                     |
| `jwt.secret`                               | JWT HS256 密钥       | -                                                |

---

## 📝 使用方式

### 1. 自媒体端（作者）

- 注册/登录自媒体账号（JWT 签发 token）
- 管理素材库（图片上传到 FastDFS）
- 创建文章（富文本，引用素材）
- 提交发布 → 系统自动加入延迟队列 → 到点自动审核
- 审核流程：文本反垃圾 → 图片鉴黄暴 → 敏感词过滤 → SimHash 去重
- 审核通过 → 文章自动静态化 → 同步 ES → App 端可见
- 审核不通过 → 查看违规原因，修改后重新提交
- 管理文章频道、查看发布状态、文章上下架

### 2. App 端（读者）

- 注册/登录账号
- 首页浏览热点文章（按频道筛选，Redis Top 30 缓存）
- 加载更多文章（上拉分页，按发布时间倒序）
- 搜索文章（ES IK 分词 + 关键词高亮 + 联想词）
- 阅读文章（FastDFS 静态 HTML，秒开）
- 点赞/不喜欢/评论/收藏/关注作者（行为实时聚合到热点）
- 查看搜索历史

### 3. 管理运维

- XXL-Job 控制台查看全量热点计算任务执行日志
- Nacos 控制台动态调整配置（阈值、权重、路由规则）
- FastDFS 控制台管理静态 HTML 文件
- ES Head / Kibana 查看索引状态和搜索性能
- Knife4j 在线调试各微服务 API
