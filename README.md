# AI-novel — 智能小说平台

在线小说平台后端服务，基于 Spring Boot 3.5 单体架构，集成了缓存分层、分布式锁、JWT 会话管理、接口幂等性、内容审核等核心能力，支撑 PC Web 与移动 H5 访问。

## 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| 语言 | Java 17 | 长期支持版本 |
| 框架 | Spring Boot 3.5 + MyBatis | 核心应用与数据访问 |
| 数据库 | MySQL 8.4 | 主存储，Flyway 管理迁移 |
| 缓存 | Caffeine + Redis 7 | L1 本地缓存 + L2 分布式缓存 |
| 安全 | Spring Security + JWT + RBAC | 认证授权与角色控制 |
| 分布式 | Redisson | 分布式锁 |
| 反向代理 | Nginx | SSL 终止、限流、静态资源 |
| 容器化 | Docker Compose | 一键启动 MySQL + Redis + 后端 + 前端 |
| CI/CD | GitHub Actions | 后端测试、前端构建、契约检查 |

## 项目结构

```
AI-novel/
├── src/main/java/com/ainovel/
│   ├── access/          # 用户接入层 — 统一响应、路径契约、幂等
│   ├── security/        # 安全治理 — JWT、RBAC、限流、风控
│   ├── user/            # 用户中心 — 注册、登录、资料、消息
│   ├── novel/           # 小说管理 — 创建、章节、发布、状态流转
│   ├── reading/         # 阅读模块 — 详情、章节、进度、缓存
│   ├── community/       # 社区互动 — 评论、点赞、关注
│   ├── recommend/       # 推荐引擎 — 首页推荐、关联推荐
│   ├── audit/           # 审核引擎 — 内容审核、人工复审
│   ├── cache/           # 缓存模块 — Caffeine + Redis + 空值防护
│   ├── persistence/     # 持久化 — MyBatis、存储抽象
│   ├── infrastructure/  # 基础设施 — 日志、监控、异步任务、异常
│   └── common/          # 公共 — 统一响应、错误码、工具类
├── src/main/resources/
│   ├── db/migration/    # Flyway 数据库迁移脚本
│   └── application*.properties
├── src/test/            # 测试代码
├── docs/                # 模块设计文档（16 篇）
├── spider/              # 爬虫工具（Python）
├── docker-compose.yaml  # 本地开发 Docker 编排
├── Dockerfile.simple    # 后端镜像构建
└── pom.xml
```

## 功能模块

| 模块 | 核心能力 |
|------|---------|
| 用户中心 | 注册、登录、JWT 刷新、资料管理、消息通知 |
| 阅读模块 | 小说详情、章节分页、阅读进度、缓存加速 |
| 小说管理 | 创建/编辑小说与章节、提审、上架/下架 |
| 缓存与并发 | 二级缓存、空值防护、互斥锁、分布式锁、幂等 |
| 持久化 | 核心表结构、MyBatis Mapper、Flyway 迁移 |
| 审核引擎 | 审核受理、人工复审、状态回写 |
| 基础设施 | TraceId、访问日志、异常处理、定时任务、Actuator |
| 用户接入层 | 统一响应、路径契约、客户端识别 |
| 安全治理 | JWT Bearer、角色放行、限流/风控待闭环 |
| 社区互动 | 评论创建/删除（查询待真实化）、点赞/关注待实现 |
| 推荐引擎 | 接口就绪，当前返回示例数据 |
| 管理端 | 作者创作主链路可走通，管理查询待真实化 |

## 快速开始

### 前置条件

- JDK 17+（完整开发包，含 `javac`）
- Docker
- Node.js 22 + npm（前端）

### 1. 启动基础设施

```bash
docker compose up -d mysql redis
```

### 2. 启动后端

```bash
./mvnw spring-boot:run
```

应用启动后 Flyway 自动执行数据库迁移。访问 http://localhost:8080/actuator/health 验证。

### 3. 启动前端（可选）

```bash
cd ../ainovel-front
npm install
npm run dev
```

### 4. 运行测试

```bash
./mvnw test
```

测试使用 H2 内存数据库，无需 MySQL/Redis。

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `AINOVEL_DB_URL` | `jdbc:mysql://127.0.0.1:3307/ainovel?...` | 数据库连接 |
| `AINOVEL_DB_USERNAME` | `ainovel` | 数据库用户 |
| `AINOVEL_REDIS_PORT` | `6380` | Redis 端口 |
| `AINOVEL_SEED_DEMO_ENABLED` | `0` | 设为 `1` 自动写入演示数据 |

Docker Compose 环境下复制 `.env.docker.local.example` → `.env.docker.local` 并修改密码。

## API 契约

- 所有接口前缀：`/api/v1/**`
- 管理端接口：`/api/v1/admin/**`
- 统一响应格式：`Result<T>`（`{ code, message, data }`）
- 错误码分层：`1000-1999` 参数错误 / `2000-2999` 认证授权 / `3000-3999` 业务错误 / `5000-5999` 系统错误
- 完整接口清单见 `docs/14-前端联调接口文档.md`

## 架构原则

1. **单体优先** — 当前以单体承载全部能力，通过包结构与服务边界保持解耦
2. **缓存优先** — 对高频读取走 L1(Caffeine) → L2(Redis) → DB 三级链路
3. **读写分治** — 写操作走事务落库，读操作优先命中缓存
4. **异步削峰** — 推荐计算、内容审核、消息通知等走异步队列

## 项目状态

当前处于 **中期开发阶段**，核心业务主链路已形成（认证 → 创作 → 审核 → 阅读）。下一阶段聚焦：补齐管理端、推荐引擎、社区互动等半成品模块，修复安全治理闭环，恢复自动化验证基线。

详见 `docs/12-项目完成情况总览.md`。

## 相关文档

- [模块文档索引](docs/README.md) — 12 篇模块详细设计文档
- [本地开发说明](docs/13-本地开发运行说明.md) — 环境搭建与故障排查
- [前端联调接口文档](docs/14-前端联调接口文档.md) — API 契约
- [项目完成情况总览](docs/12-项目完成情况总览.md) — 模块完成度矩阵与缺口分析

## 许可证

[MIT](LICENSE)
