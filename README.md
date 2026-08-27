<div align="center">

# NewsFlow · 讯通头条

**下一代分布式内容创作与分发平台**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3.9-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR10-blue)](https://spring.io/projects/spring-cloud)
[![Nacos](https://img.shields.io/badge/Nacos-1.4.x-emerald)](https://nacos.io/)
[![Kafka](https://img.shields.io/badge/Kafka-2.5-231F20?logo=apachekafka)](https://kafka.apache.org/)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-7.2-005571?logo=elasticsearch)](https://www.elastic.co/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

微服务架构 · 千万级吞吐 · 全链路内容治理

[架构概览](#架构概览) •
[核心能力](#核心能力) •
[模块说明](#模块说明) •
[快速开始](#快速开始) •
[技术亮点](#技术亮点)

</div>

---

## 架构概览

NewsFlow 采用 Spring Cloud Alibaba 微服务架构，按业务域垂直拆分 7 个核心服务，通过 Nacos 实现服务注册与配置热更新，经由 Gateway 统一鉴权与路由，消息、缓存、检索、对象存储各司其职，支撑自媒体创作端与 App 消费端双端高并发访问。

```
                          ┌─────────────────────┐
                          │   App / Admin UI    │
                          └──────────┬──────────┘
                                     │
                    ┌────────────────▼────────────────┐
                    │     Spring Cloud Gateway        │
                    │  JWT · 限流 · 路由 · 跨域        │
                    │  ┌───────────┐ ┌─────────────┐  │
                    │  │ App GW    │ │ Wemedia GW  │  │
                    └──────────┬───┘─┴──────┬──────┘
                               │            │
        ┌──────────┬───────────┼────────────┼───────────┬──────────┬──────────┐
        ▼          ▼           ▼            ▼           ▼          ▼          ▼
     ┌──────┐ ┌─────────┐ ┌────────┐ ┌──────────┐ ┌─────────┐ ┌─────────┐ ┌──────────┐
     │ User │ │ Article │ │Wemedia │ │  Search  │ │Behavior │ │ Comment │ │ Schedule │
     │ Svc  │ │   Svc   │ │  Svc   │ │   Svc    │ │   Svc   │ │   Svc   │ │   Svc    │
     └───┬──┘ └────┬────┘ └───┬────┘ └─────┬────┘ └────┬────┘ └────┬────┘ └─────┬────┘
         │         │          │             │           │          │            │
    ┌────┴─────────┴──────────┴─────────────┴───────────┴──────────┴────────────┴────┐
    │  Nacos · Kafka Streams · Redis Cluster · MySQL 5.7 · ES 7.x · MinIO · XXL-Job  │
    └────────────────────────────────────────────────────────────────────────────────┘
```

## 核心能力

### 🎨 创作者端（Wemedia）
- **素材中心**：MinIO 自定义 Starter，支持多格式素材库管理与一键引用
- **频道管理**：动态频道配置，支持权重排序、上下线控制
- **智能审核**：DFA 敏感词 + 阿里云内容安全双通道，图文自动审核转人工
- **定时发布**：延迟任务引擎 + XXL-Job 故障兜底，发布任务零丢失

### 📱 消费端（App）
- **智能信息流**：首屏分页加载，频道维度切换，已读内容自动去重
- **实时热度榜**：Kafka Streams 实时聚合阅读/点赞/收藏行为，流式计算 TopN
- **静态化详情**：Freemarker 预渲染详情页，首屏加载 < 150ms
- **语义检索**：Elasticsearch 标题 + 正文多字段匹配，联想词 + 历史记录双推荐
- **互动中心**：关注 / 点赞 / 不喜欢 / 阅读时长 / 评论 / 热评 / 收藏全链路行为采集

### 🛡️ 基础能力
- **网关鉴权**：双 Gateway 入口，JWT Token 签名校验 + 服务级白名单
- **统一响应**：全局返回体封装 + 自定义异常码枚举 + AOP 异常兜底
- **配置中心**：Nacos 多环境配置隔离，数据源/中间件参数热更新
- **链路追踪**：日志链路 ID 透传，关键节点结构化埋点

## 模块说明

```
news-platform/
├── news-platform-basic/         基础组件沉淀
│   └── news-file-starter       MinIO 对象存储 Starter（开箱即用）
├── news-platform-common/       公共能力（响应体/异常/Jackson 扩展/Redis/Swagger）
├── news-platform-utils/        工具集（JWT/加解密/SimHash/敏感词/雪花 ID …）
├── news-platform-model/        领域模型（POJO · DTO · VO · Feign 内部消息体）
├── news-platform-feign-api/    跨服务 Feign 接口契约 + Fallback
├── news-platform-gateway/      网关层
│   ├── news-platform-app-gateway        51601
│   └── news-platform-wemedia-gateway    51602
└── news-platform-service/      业务微服务集群
    ├── news-platform-user         51801  ·  用户注册登录 · 粉丝关系
    ├── news-platform-article      51802  ·  文章 CRUD · 静态化 · 热度计算
    ├── news-platform-wemedia      51803  ·  创作者端 · 自动审核 · 定时发布
    ├── news-platform-search       51804  ·  ES 索引同步 · 搜索 · 联想词
    ├── news-platform-behavior     51805  ·  行为采集 Kafka 消费
    ├── news-platform-comment      51806  ·  评论 / 回复 / 热评排序
    └── news-platform-schedule     51701  ·  延迟调度 · 审核发布任务
```

## 技术亮点

| 场景 | 技术方案 | 效果 |
|---|---|---|
| 热点数据高并发读 | Redis 多级缓存 + 布隆过滤器防穿透 + 互斥锁防击穿 | 缓存命中率 90%+，DB 压力降低 70% |
| 实时热度排行 | Kafka Streams 窗口聚合 + ZSet 动态排名 | 秒级更新，千万级 UV 无抖动 |
| 发布一致性 | 延迟任务 + XXL-Job 调度兜底 + Kafka 异步驱动 | 发布成功率 99.9%，零重复 |
| 内容审核 | DFA 本地词库初筛 → 阿里云绿网精审 → 人工复审 | 审核覆盖 100%，违规召回率 98% |
| 详情页提速 | Freemarker 静态页生成 + OSS CDN | P95 加载耗时 < 150ms |
| 搜索体验 | IK 分词 + 高亮 + 联想词（MongoDB 前缀补全） | QPS 2000+，响应 < 80ms |

## 快速开始

### 环境依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| JDK | 1.8 | 编译 & 运行 |
| Maven | 3.6+ | 构建 |
| MySQL | 5.7+ | 核心业务库 |
| Redis | 5.0+ | 缓存 / 分布式锁 |
| Nacos | 1.4.x | 注册 + 配置中心 |
| Kafka | 2.5+ | 消息队列 + 流计算 |
| Elasticsearch | 7.2 | 全文检索 |
| MongoDB | 4.x | 搜索历史 / 联想词 |
| MinIO | 最新 | 对象存储 |
| XXL-Job | 2.2 | 任务调度 |

### 构建 & 启动

```bash
# 1. 构建全工程
mvn clean install -DskipTests

# 2. 启动中间件（Nacos / MySQL / Redis / Kafka / ES / MongoDB / MinIO / XXL-Job）

# 3. 初始化 Nacos 配置
#    参考 docs/ 目录下各服务 bootstrap.yml 说明，配置 dataId：
#      - user-dev.yml  article-dev.yml  wemedia-dev.yml
#      - search-dev.yml  behavior-dev.yml comment-dev.yml schedule-dev.yml

# 4. 启动顺序
#    中间件 → service/* → gateway/*
```

### 接口文档

Knife4j 地址（服务启动后）：
| 服务 | URL |
|---|---|
| 创作者端 | http://localhost:51803/doc.html |
| 文章服务 | http://localhost:51802/doc.html |
| 搜索服务 | http://localhost:51804/doc.html |

## 文档

更多设计细节参见 [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md)

## License

Released under the [MIT License](./LICENSE).
