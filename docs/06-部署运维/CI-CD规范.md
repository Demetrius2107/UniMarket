# CI/CD 规范 — UniMarket

> 适用于 UniMarket 统一商城前台的持续集成/持续部署流水线规范：构建、测试、质量门禁、发布。
> 目标：每次提交自动验证，合并自动构建，发布可追溯可回滚。
> 版本：v1.0 ｜ 2026-08-10

---

## 目录

1. [总体原则](#1-总体原则)
2. [流水线阶段设计](#2-流水线阶段设计)
3. [CI 触发策略](#3-ci-触发策略)
4. [质量门禁](#4-质量门禁)
5. [制品与版本管理](#5-制品与版本管理)
6. [CD 部署策略](#6-cd-部署策略)
7. [GitHub Actions 模板](#7-github-actions-模板)
8. [失败处理与告警](#8-失败处理与告警)
9. [检查清单](#9-检查清单)

---

## 1. 总体原则

| 原则 | 说明 |
|------|------|
| 提交即验证 | 每次 push 自动跑构建 + 测试 + 静态检查 |
| 门禁前置 | 质量不达标不合并、不发布 |
| 制品唯一 | 每次构建产物带版本号，可追溯源码 commit |
| 发布可回滚 | 部署自动记录版本，支持一键回滚上一版本 |
| 配置不入库 | 环境配置/密钥走变量/配置中心，不进仓库 |

**范围**：后端 Maven 多模块（主）+ 前端 SPA（可选接入）。

---

## 2. 流水线阶段设计

```
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ 检出   │→│ 构建    │→│ 测试    │→│ 质量门禁 │→│ 制品    │→│ 部署    │
│ checkout│ │ compile│ │ test    │ │ 检查/安全│ │ 打包上传 │ │ 发布/回滚│
└────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘
```

| 阶段 | 内容 | 失败处理 |
|------|------|----------|
| 检出 | 拉取分支代码 | 中断 |
| 构建 | `mvn clean compile`（或 package -DskipTests） | 中断 |
| 测试 | `mvn test`（JUnit + JaCoCo 覆盖率） | 中断 |
| 质量门禁 | Checkstyle / SpotBugs / OWASP 依赖扫描 / 覆盖率阈值 | 中断或告警（按级别） |
| 制品 | 打包 + 上传制品库（或本地归档），带版本号 | 中断 |
| 部署 | 按环境部署（dev/test/prod），记录版本 | 回滚上一版本 |

---

## 3. CI 触发策略

| 事件 | 动作 |
|------|------|
| push 到 `feat/*` / `fix/*` | 跑构建 + 测试（快速反馈） |
| push 到 `master` | 全量流水线（构建 + 测试 + 质量门禁 + 制品） |
| tag `v*` | 全量流水线 + 发布部署（prod 可选） |
| 定时（可选） | 每日依赖漏洞扫描 |
| 手动 | 支持 workflow_dispatch 手动触发指定阶段 |

**约定**：禁止 CI 里跑需要外部凭据的长耗时才做（如真实支付联调）；第三方联调走手动/独立 job。

---

## 4. 质量门禁

| 门禁 | 工具 | 阈值 |
|------|------|------|
| 代码风格 | Checkstyle（阿里规约插件可选） | 0 Error |
| 静态分析 | SpotBugs / PMD | 0 Critical / 0 High 新增 |
| 覆盖率 | JaCoCo | 新增代码行覆盖 ≥ 70%（核心模块 ≥ 90%，见《测试规范》§7） |
| 依赖漏洞 | OWASP Dependency-Check | 0 高危未豁免 |
| 构建 | Maven | 0 编译错误 |

**门禁处理**：Critical/High 未处理 → 构建失败；中低危 → 告警 + 限期处理登记。

```xml
<!-- pom.xml 质量插件（父 POM 统一配置） -->
<plugin>
  <groupId>org.jacoco</groupId>
  <artifactId>jacoco-maven-plugin</artifactId>
  <version>0.8.11</version>
  <executions>
    <execution><goals><goal>report</goal></goals></execution>
  </executions>
</plugin>
```

---

## 5. 制品与版本管理

| 项 | 约定 |
|----|------|
| 版本号 | Maven 版本遵循语义化：`1.0.0`；里程碑打 tag `v0.1.0`（见《分支开发规范》§3.4） |
| 制品名 | `uni-market-app-<version>.jar`，含 git commit 短 hash（`maven.buildNumber`） |
| 制品存储 | 本地归档 `artifacts/` 或私有制品库（Nexus 可选）；保留最近 N 个 |
| 元信息 | 制品关联：版本 → commit → 构建时间 → 部署环境，可追溯 |

---

## 6. CD 部署策略

| 环境 | 策略 |
|------|------|
| dev | 每次 master 构建后自动部署（若环境允许） |
| test | 手动触发部署（联调环境，避免频繁重启） |
| prod | 手动 + 审批；打 tag 后部署 |

**部署方式**（本系统）：
- 演示环境：构建产物 jar → SCP 到服务器 → 启动脚本（`deploy.sh`，含启动/停止/日志）。
- 后续可容器化：Docker 镜像 + docker-compose 滚动更新。

```bash
# deploy.sh 要点（演示环境）
# 1. 备份当前版本（artifacts/<version>.jar.bak）
# 2. 上传新 jar
# 3. 停旧进程 → 启新进程（nohup java -jar ...）
# 4. 健康检查：curl /api/v1/health 通过则成功，否则回滚备份版本
```

**回滚**：保留上一版本制品；`deploy.sh rollback` 一键恢复。

---

## 7. GitHub Actions 模板

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [master]
    tags: ["v*"]
  pull_request:
    branches: [master]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 8
        uses: actions/setup-java@v4
        with:
          java-version: "8"
          distribution: "temurin"

      - name: Build & Test
        run: mvn clean package -DskipTests=false

      - name: Coverage Report
        run: mvn jacoco:report

      - name: Dependency Check
        run: mvn org.owasp:dependency-check-maven:check \
             -DfailBuildOnCVSS=8    # CVSS ≥ 8 构建失败

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: uni-market-app
          path: uni-market-app/target/*.jar
          retention-days: 30
```

> 依赖漏洞扫描较慢，可拆为独立 job（定时执行）；CI 配置遵循《分支开发规范》类型 `ci`。

---

## 8. 失败处理与告警

| 失败类型 | 处理 |
|----------|------|
| 编译/测试失败 | 阻止合并（PR 状态 failed），作者修复 |
| 质量门禁失败 | 阻止合并，登记缺陷 |
| 依赖高危 | 阻止合并，升级或登记豁免（附理由） |
| 部署失败 | 自动回滚上一版本 + 告警 |
| 流水线异常（环境问题） | 重跑，连续失败告警运维 |

**告警通道**：GitHub 通知 / 邮件（可选企业微信/钉钉机器人 webhook）。

---

## 9. 检查清单

- [ ] 每次 push 自动构建 + 测试，失败阻止合并
- [ ] master 合并/打 tag 触发全量流水线
- [ ] 质量门禁：Checkstyle 0 Error、覆盖率达标、依赖高危为 0
- [ ] 制品带版本号 + commit，可追溯
- [ ] 部署记录版本，支持一键回滚
- [ ] 密钥/配置不进仓库（GitHub Secrets / 环境变量）
- [ ] CI 配置入库（.github/workflows），文档同步

---

*CI/CD 规范 v1.0 — 2026-08-10，配套《分支开发规范》与《部署手册》使用*
