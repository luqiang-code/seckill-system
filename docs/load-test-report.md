# 秒杀系统压测报告

**日期**: 2026-05-22  
**环境**: 本地开发机 (Mac, Colima Docker)  
**应用**: Spring Boot 3.2.5, Java 26, Tomcat (max-threads=500)  
**测试工具**: JMeter 5.6.3

---

## 1. 测试环境

| 组件 | 配置 |
|---|---|
| 应用实例 | 1 (localhost:8080) |
| Tomcat 线程池 | max=500, min-spare=50 |
| MySQL | 8.0, HikariCP max=30 |
| Redis | 7-alpine, Sentinel 模式 (1 master + 1 replica + 3 sentinel) |
| 限流 | TokenBucketRateLimiter(5000 req/s) |
| JWT 鉴权 | 已移除 (当前阶段未启用) |

---

## 2. 测试场景

**接口**: `POST /seckill/do/1?userId={userId}`

**线程组配置**:
- 并发线程: 200
- Ramp-up: 5 秒
- 持续时间: 60 秒 / 30 秒
- 用户池: 10 万唯一 userId (CSV 数据文件)

---

## 3. 压测结果

### 3.1 核心指标 (200线程 × 60秒)

| 指标 | 数值 |
|---|---|
| **总请求数** | 130,968 |
| **吞吐量 (QPS)** | **2,176 req/s** |
| **平均响应时间** | 86 ms |
| **最小响应时间** | 19 ms |
| **最大响应时间** | 1,270 ms |
| **HTTP 错误率** | 0% |
| **库存消耗** | 8,783 → 0 (完全售罄) |

### 3.2 响应时间分布

| 阶段 | 时间段 | 吞吐量 | 平均延迟 | 备注 |
|---|---|---|---|---|
| 预热期 | 0-23s | 1,070/s | 165ms | 首次请求触发库存预热、JIT 编译 |
| 稳定期 | 23-53s | 2,863/s | 68ms | 本地库存缓存命中，系统进入稳态 |
| 收尾期 | 53-60s | 2,903/s | 67ms | 最高吞吐区间 |

### 3.3 售罄后快速通道

库存归零后，售罄标记 (`soldout:1`) 短路，请求在布隆过滤器之后直接返回"库存不足"：

| 指标 | 售罄前 (正常链路) | 售罄后 (快速通道) |
|---|---|---|
| 平均延迟 | ~86ms | ~14ms (估) |
| 吞吐量 | ~2,176/s | ~11,600/s (估) |

### 3.4 客户端限制

两次短时高库存压测中，JMeter 客户端出现 `java.net.BindException`（本地端口耗尽）：

| 配置 | 总请求 | 吞吐量 | 客户端错误率 |
|---|---|---|---|
| keepalive=true | 22,500 | 742/s | 12.7% |
| keepalive=false | 30,352 | 999/s | 47.1% |

> 注意: 单机 JMeter 200 线程在本地回环上实测上限约 1,000-2,000 req/s（受限于 ephemeral port 范围 + TIME_WAIT）。服务端实际 QPS 高于此值，建议后续使用 `wrk` 或分布式 JMeter 进行更大压力压测。

---

## 4. 架构瓶颈分析

### 4.1 当前限流器

`TokenBucketRateLimiter(5000)` 即理论上限 5,000 req/s。当前实测 ~2,200 req/s，尚未触及限流阈值。

### 4.2 链路耗时拆解 (估算)

| 阶段 | 操作 | 预估耗时 |
|---|---|---|
| 布隆过滤器 | 内存位图查询 | <1ms |
| 售罄标记检查 | `redisTemplate.hasKey()` | ~1-2ms |
| 本地库存获取 | `localStockCache.acquire()` (命中) | <1ms |
| Redis Pipeline | SADD + EXPIRE + LPUSH + EXPIRE | ~2-5ms |
| 网络 + 序列化 | HTTP / JSON | ~5-10ms |

### 4.3 观察

- Tomcat 500 线程 + HikariCP 30 连接未成为瓶颈
- Redis Lettuce 连接池 (max-active=100) 充足，Pipeline 减少 RTT 是关键优化
- 本地库存缓存 (LocalStockCache) 有效降低了 Redis 交互频率
- 售罄标记快速短路避免了库存归零后的无效分片遍历

---

## 5. 建议

1. **压测工具升级**: 换 `wrk` 或 `hey` 替代单机 JMeter，消除客户端端口瓶颈
2. **分布式压测**: 多机 JMeter Slave + Master 模式，突破单机网络限制
3. **限流调整**: 当前 5000/s 硬上限，建议改为可配置的动态限流
4. **监控补齐**: 压测时接入 Prometheus + Grafana，实时观察 JVM/Redis/DB 指标
5. **预热优化**: 首次请求预热库存有 165ms 毛刺，可考虑启动时全量预热
