# 后端 Bug 记录

## 2026-03-27

本次排查聚焦在本地 WSL 开发链路和后续服务器部署链路。实际确认了
3 类问题：Flyway 的 MySQL 数据库支持模块缺失导致后端无法启动、首次
下载新依赖时 Maven 到 Maven Central 的 TLS 握手异常，以及 Mockito 在
WSL 环境下无法自附加 Byte Buddy agent 导致测试大面积失败。下面记录
现象、根因和部署修正项。

### 1) 后端启动失败：`Unsupported Database: MySQL 8.4`

这个问题是本次后端无法启动的直接根因。应用已经可以连上 Redis 和
MySQL，但会在 Flyway 初始化阶段退出。

- 现象：执行 `./mvnw spring-boot:run` 时，日志显示 Redisson 与 Hikari
  已成功连接 Redis 和 MySQL，随后在 `flywayInitializer` 创建阶段报错
  `Unsupported Database: MySQL 8.4`，Spring Boot 启动失败。
- 根因：
  - `docker-compose.yaml` 使用浮动镜像 `mysql:8`，当前实际拉起的是
    `MySQL 8.4`。
  - 项目 `pom.xml` 里只有 `org.flywaydb:flyway-core`，缺少
    `org.flywaydb:flyway-mysql`。
  - 在 Spring Boot `3.5.7` 对应的 Flyway `11.7.2` 组合下，MySQL 支持
    已拆分到独立模块；缺失该模块时，Flyway 无法识别当前 MySQL。
- 修复：
  - 在 `pom.xml` 中增加 `org.flywaydb:flyway-mysql`，版本继续跟随
    Spring Boot 的依赖管理。
  - 新增回归测试 `FlywayMysqlSupportTests`，校验运行时 classpath 上
    存在 Flyway 的 MySQL 插件。
  - 修复后再次执行 `./mvnw spring-boot:run`，应用已成功启动；Flyway
    不再报 `Unsupported Database`，而是正常识别 `MySQL 8.4` 并完成迁
    移。
- 部署修正项：
  - 服务器上的构建产物必须包含 `org.flywaydb:flyway-mysql`，不能只保
    留 `flyway-core`。
  - 如果服务器也使用 `mysql:8` 这样的浮动标签，后续可能继续漂移到更
    新的小版本。建议固定成明确版本，或者随 Spring Boot 一起升级
    Flyway，避免数据库版本继续向前漂移。
  - 修复后启动日志仍会提示 `MySQL 8.4` 高于当前 Flyway 已测试的最
    新版本 `8.1`。这个告警当前不会阻断启动，但部署时仍建议关注
    Spring Boot / Flyway 的后续升级。

### 2) 新依赖首次下载失败：`Remote host terminated the handshake`

这个问题不是业务代码缺陷，但会直接影响干净机器上的首次构建和服务器
部署。

- 现象：在增加 `flyway-mysql` 后，首次执行 Maven 构建时报错
  `Could not transfer artifact org.flywaydb:flyway-mysql...`
  和 `Remote host terminated the handshake`。
- 根因：
  - 当前 Maven / JDK 到 `repo.maven.apache.org` 的 HTTPS/TLS 下载链
    路不稳定。
  - 同一地址用 `curl -I` 可以正常返回 `HTTP/2 200`，说明不是依赖不存
    在，而是 Maven 所在 Java TLS 链路或代理配置存在环境问题。
- 修复：
  - 本地为了继续验证，临时把 `flyway-mysql-11.7.2` 的 `pom` 和 `jar`
    下载到了 `~/.m2/repository`。
- 部署修正项：
  - 在服务器上先验证
    `curl -I https://repo.maven.apache.org/maven2/.../flyway-mysql-11.7.2.pom`
    是否可达。
  - 如果服务器走代理或内网仓库，提前配置 `~/.m2/settings.xml` 的
    `mirror` / `proxy`，不要依赖默认直连。
  - 检查服务器时间、CA 证书、JDK 证书库和出站 HTTPS 策略，确保
    Maven 能稳定访问 Maven Central。
  - 如果生产环境网络受限，建议在 CI 或制品库中预热这些依赖，而不是
    把首次下载留到部署阶段。

### 3) 测试失败：Mockito 无法初始化 inline `MockMaker`

这个问题不会阻止后端运行，但会阻止 `./mvnw test` 在当前 WSL / Linux
环境下通过，属于部署前必须处理的测试链路问题。

- 现象：执行 `./mvnw test` 时，共有 61 个 `error`。底层异常为
  `Could not initialize plugin: interface org.mockito.plugins.MockMaker`，
  进一步展开后可见：
  `Could not initialize inline Byte Buddy mock maker`，
  `It appears as if your JDK does not supply a working agent attachment mechanism`，
  以及 `AttachNotSupportedException` / `Can not attach to current VM`。
- 根因：
  - 当前测试运行环境是 WSL2 + Eclipse Adoptium JDK `17.0.9`。
  - Mockito 默认使用 inline `MockMaker`，它依赖 Byte Buddy 在测试
    运行时向当前 JVM 自附加 agent。
  - 当前环境的 attach 机制不可用，导致所有依赖 Mockito inline mock
    maker 的测试在 Spring 上下文初始化前就失败。
- 已验证的修复办法：
  - 单独增加 `-Djdk.attach.allowAttachSelf=true` 或
    `-XX:+EnableDynamicAgentLoading` 无法解决该问题。
  - 预加载 `byte-buddy-agent` 可以通过验证。实测命令如下：
    `./mvnw -Dtest=AuthSessionServiceFailClosedTests test -DargLine='-javaagent:/home/zj-zhuo/.m2/repository/net/bytebuddy/byte-buddy-agent/1.17.8/byte-buddy-agent-1.17.8.jar'`
  - 上述命令在当前环境下已成功通过，说明核心问题是“自附加失败”，而
    不是 Mockito 依赖缺失。
- 部署修正项：
  - 如果服务器或 CI 也运行在类似的受限 Linux / 容器环境，建议把
    `byte-buddy-agent` 的 `-javaagent` 固化到
    `maven-surefire-plugin` 的 `argLine` 中，而不是依赖运行时自附加。
  - 当前 Spring Boot 父 POM 已暴露 `byte-buddy.version=1.17.8`，可在
    Maven 配置里组合 `${settings.localRepository}` 和
    `${byte-buddy.version}` 生成 agent 路径，避免写死开发机路径。
  - 如果后续改造测试体系，也可以评估减少对 inline mock maker 的依赖，
    但在当前代码状态下，预加载 `byte-buddy-agent` 是已验证有效的修复
    方案。

## 2026-03-23

### 1) 本地启动失败：`找不到或无法加载主类 com.ainovel.AInovelApplication`
- 现象：在 VS Code / Java 启动配置中直接运行后端时，命令行报错 `错误: 找不到或无法加载主类 com.ainovel.AInovelApplication`，并伴随 `java.lang.ClassNotFoundException`。
- 根因：启动前没有先完成有效编译，`target/classes/com/ainovel/AInovelApplication.class` 未生成；进一步排查发现，触发场景通常是当前环境只接入了 `java` 运行时、没有可用的 `javac`，或者 IDE 未先执行 Maven 编译。
- 修复：
  - `mvnw` / `mvnw.cmd` 增加完整 JDK 校验，缺少 `javac` 时直接给出明确错误，而不是拖到运行期报主类找不到。
  - 根目录 `.vscode/launch.json` 增加后端预编译任务，运行 `AInovelApplication` 前先执行 `mvnw compile`。
  - `docs/13-本地开发运行说明.md` 补充了 `JAVA_HOME`、`javac` 与 VS Code 清理/重导入排查步骤。
- 影响：仅影响本地开发启动链路，不涉及业务接口和数据库结构。

## 2026-03-22

### 1) Maven 编译失败：`release version 17 not supported`
- 现象：执行 `./mvnw test` 在 `maven-compiler-plugin` 阶段失败，报错 `release version 17 not supported`。
- 根因：当前运行环境的 `javac` 不支持 `--release` 参数，`pom.xml` 中使用 `<release>` 导致编译直接失败。
- 修复：将编译参数从 `<release>` 调整为 `<source>/<target>`，版本保持 `17`。
- 影响：仅影响构建配置，不涉及接口变更。

### 2) 刷新令牌误判无效：`refresh token invalid`
- 现象：`CoreBusinessFlowTests.shouldCompleteCoreBusinessFlow` 在刷新令牌时抛出 `BusinessException(refresh token invalid)`。
- 根因：刷新请求未携带 `deviceId` 时，`matchesDevice` 把空设备归一化为 `"unknown"` 后与会话设备严格比较，导致误判失败。
- 修复：`matchesDevice` 调整为“仅当请求显式携带 `deviceId` 时进行一致性校验”；未携带时兼容放行。
- 影响：兼容历史/未上报设备 ID 的客户端；当客户端传入设备 ID 时仍保持严格匹配，不改变接口。
