# 秒杀系统压测报告 — 2026-06-02

**日期:** 2026-06-02
**配置:** 10 段库存，JWT 认证，`stress` 隔离环境
**JMeter 测试计划:** `jmeter/seckill-stress-test.jmx`

## 1. 测试环境

| 组件 | 配置 |
|------|------|
| Host | Mac (Apple Silicon), JMeter 5.6.3 |
| 应用 | Spring Boot 3.x, `java -jar` 直接运行 (非 Docker) |
| Profile | `stress` — 隔离数据库 `seckill_stress` (MySQL 3307) |
| Redis | Docker 容器, 1 主 + Sentinel |
| MySQL | Docker 容器, 端口 3307, 独立 `seckill_stress` 库 |
| 线程限制 | macOS `ulimit -u` 硬限制 1392 |
| 监控 | 同源部署 `dashboard.html`，250ms 轮询，实时 QPS |

## 2. 压测结果

每个线程执行 2 个请求：`POST /auth/test-token` (登录) + `POST /seckill/do/1` (秒杀)。

| 线程数 | Ramp | 总请求 | 吞吐量 | 秒杀 QPS (估) | 平均延迟 | 最大延迟 | 错误 |
|--------|------|--------|--------|---------------|----------|----------|------|
| 500 | 3s | 1000 | 327/s | ~150 | 246ms | 893ms | 0 |
| 2000 | 5s | 4000 | 557/s | ~280 | 1098ms | 3358ms | 0 |
| 2000 | 2s | 4000 | 539/s | ~270 | 1655ms | 4468ms | 0 |
| 5000 | 5s | ❌ | - | - | - | - | JMeter OOM |

## 3. 瓶颈分析

- **JMeter 端瓶颈：** macOS `ulimit -u` 硬限制 1392，JMeter 在 ~2000 线程时触发 `pthread_create failed (EAGAIN)`，无法进一步提升客户端并发
- **应用端未达瓶颈：** 所有成功测试 0 错误，秒杀系统自身仍有余量
- **吞吐量趋势：** 500→2000 线程提升 1.7× (327→557/s)，吞吐增长低于线程增长，说明 Redis Lua 竞争随并发增加而加剧

## 4. 与历史对比

| 日期 | 部署方式 | 认证 | 500 并发 QPS | 1000 并发 QPS | 2000 并发 |
|------|----------|------|-------------|-------------|-----------|
| 2026-05-14 (优化前) | Docker | 无 | 162/s | 148/s | ❌ Crash |
| 2026-05-14 (优化后) | Docker | 无 | 912/s | 787/s | ❌ VM 崩溃 |
| **2026-06-02** | **本机 JAR** | **JWT** | **~300/s** | - | **~540/s** |

> 本次含 JWT 认证（每线程额外 1 次 HTTP 调用），纯秒杀 QPS 约为总吞吐的一半。与历史 Docker 部署相比，本机直接运行消除了容器网络开销，但 JWT 认证增加了额外耗时。

## 5. 环境隔离

压测使用 `application-stress.yml` profile，完全隔离生产环境：

| 维度 | 生产 | 压测 |
|------|------|------|
| 数据库 | `seckill` (3306) | `seckill_stress` (3307) |
| 库存初始值 | 100 | 10000 |
| Profile | `default` | `stress` |

## 6. 监控面板功能

- 剩余库存 / 本轮已售 / 预估 QPS / 累计售出 — 四卡片实时刷新
- 库存消耗进度条（已售/剩余百分比）
- 补货自动检测：库存回升时自动重置本轮基线，累计售出保留
- 售罄自动识别
- 250ms 轮询间隔，QPS 基于库存差值/时间间隔实时计算

## 7. 测试命令

```bash
# 启动后端 (压测环境)
java -jar target/seckill-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=stress

# 执行压测
jmeter -n -t jmeter/seckill-stress-test.jmx -l jmeter/results.jtl

# 监控面板
open http://localhost:8080/dashboard.html
```
