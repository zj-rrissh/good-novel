# 后端 Bug 记录

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
