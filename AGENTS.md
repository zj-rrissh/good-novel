# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven-based Spring Boot application. Main code lives in `src/main/java/com/ainovel`, with the entry point in `AInovelApplication.java`. Configuration files belong under `src/main/resources`, currently centered on `application.properties`. Tests live in `src/test/java/com/ainovel`. Root-level documents such as [智能小说平台模块方案设计.md](/mnt/d/myprogram/ai-novel/ainovel/智能小说平台模块方案设计.md) and `HELP.md` describe the target architecture and framework references. Treat `drawio.png` as a design asset, not application runtime content.

## Build, Test, and Development Commands
Use the Maven wrapper so local Maven installation is optional:

- `./mvnw clean test`: compile and run the JUnit test suite.
- `./mvnw spring-boot:run`: start the app locally with the default profile.
- `./mvnw clean package`: build the runnable JAR under `target/`.

On Windows, use `mvnw.cmd` instead of `./mvnw`.

## Coding Style & Naming Conventions
Use Java 17 and standard Spring Boot conventions. Indent with 4 spaces, keep one public class per file, and prefer constructor injection for Spring beans. Class names use `PascalCase`; methods and fields use `camelCase`; constants use `UPPER_SNAKE_CASE`. Package names stay lowercase, grouped by domain such as `auth`, `user`, `novel`, and `reading`. No formatter or linter is configured yet, so keep imports clean and avoid unused code before committing.

## Testing Guidelines
Tests use JUnit 5 via `spring-boot-starter-test`. Name test classes `*Tests` and test methods with clear behavior-focused names such as `shouldCreateNovelWhenInputIsValid`. Add unit or slice tests for new service, controller, and repository logic; do not rely only on `contextLoads()`. Run `./mvnw clean test` before opening a PR.

## Commit & Pull Request Guidelines
Git history is not available in the current workspace, so no existing commit convention could be verified. Use short, imperative commit messages such as `feat: add novel creation endpoint` or `fix: guard missing chapter cache`. PRs should include a concise summary, impacted modules, test evidence, and any config or schema changes. Include request/response examples when API behavior changes.

## Security & Configuration Tips
Do not commit real secrets or environment-specific credentials to `application.properties`. Prefer environment variables or profile-specific overrides for MySQL, Redis, and token settings. When adding new endpoints, document required authentication and rate-limiting assumptions alongside the code.
