# Vue 前端迁移计划

## 目标

将现有两个独立的静态 HTML 页面（`seckill.html` + `dashboard.html`）迁移至 Vue 3 + Vite + TypeScript 单页应用。

## 现状

| 文件 | 行数 | 功能 | 技术 |
|---|---|---|---|
| `src/main/resources/static/seckill.html` | 265 | 秒杀抢购 | 原生 JS + DOM 操作，200ms 倒计时轮询，fetch API |
| `src/main/resources/static/dashboard.html` | 262 | 压测监控 | 原生 JS，150ms 状态轮询，并发请求模拟 |

两文件共享零代码，各自硬编码 API 调用和状态管理。

## 技术选型

- **Vue 3** (Composition API + `<script setup>`)
- **Vite** (开发服务器 + 构建)
- **TypeScript**
- **vue-router** (SPA 路由)
- **不引入** Pinia/Vuex（状态简单，composables 足够）

## 项目结构

```
seckill-web/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/
│   │   └── index.ts              # fetch 封装，统一处理 ApiResponse {code, message, data}
│   ├── composables/
│   │   ├── useCountdown.ts       # 秒杀窗口倒计时
│   │   ├── usePolling.ts         # 通用轮询 hook
│   │   └── useSeckill.ts         # 秒杀操作 + 防抖
│   ├── views/
│   │   ├── SeckillView.vue       # 秒杀抢购页
│   │   └── DashboardView.vue     # 压测监控页
│   ├── components/
│   │   ├── CountdownBar.vue
│   │   ├── GoodsCard.vue
│   │   ├── GoodsGrid.vue
│   │   ├── UserBar.vue
│   │   ├── StatCard.vue
│   │   ├── ProgressBar.vue
│   │   └── LogConsole.vue
│   ├── router/
│   │   └── index.ts              # / → SeckillView, /dashboard → DashboardView
│   └── styles/
│       └── main.css
```

## 实施步骤

### Step 1: 脚手架
- `npm create vite@latest seckill-web -- --template vue-ts`
- 配置 `vite.config.ts` 代理到 `localhost:8080`
- 验证 HMR 可用

### Step 2: API 层
- 实现 `api/index.ts`：统一 fetch 封装，解包 `ApiResponse<T>`，处理 429
- TypeScript 类型定义：`Goods`、`OrderInfo`、`ApiResponse<T>`

### Step 3: useCountdown composable
- 输入：`windowDuration`, `intervalDuration`
- 输出：`remaining`, `status`, `isWindowOpen`
- 基于 `requestAnimationFrame`，自动清理

### Step 4: 核心组件
- `CountdownBar.vue` — 倒计时展示
- `GoodsCard.vue` — 单个商品卡片 + 秒杀按钮 + 结果展示
- `GoodsGrid.vue` — 商品列表容器
- `UserBar.vue` — 用户 ID 输入

### Step 5: 组装 SeckillView
- 集成 `useCountdown` + `usePolling`（商品列表 5s 刷新）
- 集成 `useSeckill`（秒杀请求 + 1s 防抖 + 结果轮询）
- 串通完整秒杀流程

### Step 6: usePolling composable
- 通用轮询：`url`, `interval`, `immediate` → `data`, `error`, `isActive`
- 替代两处手写轮询

### Step 7: 迁移 Dashboard
- `StatCard.vue` × 4（库存/售出/请求/QPS）
- `ProgressBar.vue` × 3（库存消耗/QPS 进度）
- `LogConsole.vue`
- `DashboardView.vue` 组装

### Step 8: 路由
- `/` → `SeckillView`
- `/dashboard` → `DashboardView`
- 404 fallback

### Step 9: 集成到 Spring Boot
- `vite build` 输出到 `src/main/resources/static/`
- 删除旧 `seckill.html` 和 `dashboard.html`
- `WebConfig.java` 添加 SPA history mode fallback

## 不变项

- 所有后端 Controller / Service / Interceptor 不动
- API 响应格式 `ApiResponse<T>` 不动
- Redis/MySQL/并发优化逻辑不动
- JWT 认证流程不动

## 预计总工时

4-5 小时。
