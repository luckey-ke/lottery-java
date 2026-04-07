# 🎰 彩票数据采集与统计分析系统

Java 17 + Spring Boot 3 后端 + Vue 3 前端

## 支持彩种

| 彩种 | 代号 | 开奖日 |
|------|------|--------|
| 双色球 | `ssq` | 周一、三、六 |
| 大乐透 | `dlt` | 周一、三、六 |
| 福彩3D | `fc3d` | 每天 |
| 排列三 | `pl3` | 每天 |
| 排列五 | `pl5` | 每天 |
| 七乐彩 | `qlc` | 周一、三、五 |

## 功能概览

### 📊 数据总览
- 各彩种数据状态、最新开奖号码
- 分页浏览开奖记录，含销售额、一二等奖详情

### ⚙️ 数据管理
- 手动拉取全部/指定彩种数据（支持最新N期、近1年、全部历史）
- 异步任务进度跟踪
- 每天 22:30 定时自动拉取

### 📈 统计分析
- **号码频率** — 每个号码出现次数（柱状图）
- **遗漏值** — 每个号码已多少期未出现
- **热号/冷号** — TOP10 / 末10
- **和值统计** — 平均值、最大值、最小值、极差
- **跨度分析** — 最大号减最小号，含分布图
- **AC值** — 号码复杂度指数（不同差值个数 - (n-1)）
- **连号分布** — 相邻号码对数统计
- **重号统计** — 与上期重复号码数均值
- **奇偶比/大小比** — 号码奇偶、大小比例分布
- **质合比** — 质数/合数比例分布
- **012路** — 除3余数分布
- **区间比** — 三区分区统计
- **和尾分布** — 和值个位数统计
- **龙虎和** — 首尾号比较（3D/排列系列）
- **高频组合** — TOP20 出现最多的组合（位置型）

### 🔥 趋势分析
- 和值走势、跨度走势、AC值走势（折线图）
- 奇偶/大小/质数个数走势
- 蓝球走势、三区分布走势
- 连号/重号趋势对比

### 🎯 号码推荐
基于历史数据与统计分析，每日推荐5组号码，5种策略：

| 策略 | 逻辑 |
|------|------|
| ⚖️ 均衡策略 | 热号+冷号+温号均衡搭配 |
| 🔥 热号追踪 | 侧重高频号码，跟踪近期趋势 |
| ❄️ 冷号回补 | 侧重遗漏值大的号码 |
| 📊 统计最优 | 综合评分，加权排序 |
| 🎲 随机精选 | 随机生成，满足AC值/跨度约束 |

**智能特性：**
- 同一天推荐结果固定（基于日期种子）
- 自动记录每日推荐到数据库
- 自动对比开奖结果，计算各策略命中率
- 根据历史命中率动态调整推荐权重
- 支持复制文本、导出TXT、生成图片（预览后下载）

### 📋 推荐历史
- 按天查看历史推荐记录
- 显示推荐号码 vs 开奖号码 vs 命中数
- 分页浏览

### 📊 命中统计
- 各策略命中率柱状图
- 平均/最大命中主号数、副号数
- 系统自动根据命中率调整权重

## 快速启动

### 后端 (Java 17 + Spring Boot 3)

```bash
cd backend

# 编译运行
mvn spring-boot:run

# 后端启动在 http://localhost:8080
# 首次启动自动建表（data/lottery.db）
```

### 前端 (Vue 3 + Vite)

```bash
cd frontend

# 安装依赖
npm install

# 开发模式
npm run dev

# 前端启动在 http://localhost:3000
# 已配置代理到后端 8080
```

### 拉取数据

后端启动后，在前端「管理」页面点击拉取，或直接调用 API：

```bash
# 拉取全部彩种最新1期
curl -X POST "http://localhost:8080/api/lottery/fetch"

# 拉取双色球全部历史
curl -X POST "http://localhost:8080/api/lottery/fetch/ssq?scope=all"

# 查看状态
curl http://localhost:8080/api/lottery/status
```

## API 接口

### 数据接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/lottery/status` | 数据统计状态 |
| GET | `/api/lottery/results?type=ssq&limit=20` | 查询开奖记录 |
| GET | `/api/lottery/latest?type=ssq` | 最新一期期号 |
| POST | `/api/lottery/fetch?scope=latest-1` | 拉取全部彩种 |
| POST | `/api/lottery/fetch/{type}?scope=all` | 拉取指定彩种 |
| GET | `/api/lottery/fetch/tasks/{taskId}` | 查询任务进度 |
| GET | `/api/lottery/fetch/history` | 查询抓取历史 |
| GET | `/api/lottery/fetch/history/{taskId}` | 查询抓取详情 |

### 分析接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/lottery/analyze?type=ssq` | 统计分析（全维度） |
| GET | `/api/lottery/trend?type=ssq&n=30` | 趋势分析 |

### 推荐接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/lottery/recommend?type=ssq` | 每日号码推荐（5组） |
| GET | `/api/lottery/recommend/history?type=ssq` | 推荐历史记录 |
| GET | `/api/lottery/recommend/stats?type=ssq` | 推荐命中率统计 |

## 自动化

内置定时任务：每天 22:30 自动拉取当日有开奖的彩种。

修改时间：编辑 `application.yml` 中 `lottery.fetch.hour/minute`，或改 `DailyFetchTask.java` 中的 cron 表达式。

## 切换 MySQL

1. 修改 `backend/src/main/resources/application.yml`：
   - 注释掉 SQLite 配置
   - 取消注释 MySQL 配置
   - 设置环境变量 `MYSQL_HOST`、`MYSQL_USER`、`MYSQL_PASSWORD`、`MYSQL_DB`

2. 建库：
   ```sql
   CREATE DATABASE lottery CHARACTER SET utf8mb4;
   ```

3. 修改 `schema.sql` 中的建表语句（`AUTOINCREMENT` → `AUTO_INCREMENT` 等）

## 项目结构

```
lottery-java/
├── backend/                                    # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/lottery/
│       │   ├── LotteryApplication.java
│       │   ├── config/
│       │   │   ├── CorsConfig.java
│       │   │   └── SchemaMigrationRunner.java
│       │   ├── controller/
│       │   │   └── LotteryController.java
│       │   ├── entity/
│       │   │   ├── LotteryResult.java
│       │   │   ├── LotteryType.java
│       │   │   ├── FetchHistoryTask.java
│       │   │   ├── FetchHistoryDetail.java
│       │   │   └── RecommendationHistory.java
│       │   ├── mapper/
│       │   │   ├── LotteryResultMapper.java
│       │   │   ├── FetchHistoryMapper.java
│       │   │   └── RecommendationHistoryMapper.java
│       │   ├── service/
│       │   │   ├── LotteryResultService.java
│       │   │   ├── FetchService.java
│       │   │   ├── FetchHistoryService.java
│       │   │   ├── AnalysisService.java
│       │   │   ├── RecommendationService.java
│       │   │   ├── RecommendationHistoryService.java
│       │   │   └── impl/FetchServiceImpl.java
│       │   └── task/
│       │       └── DailyFetchTask.java
│       └── resources/
│           ├── application.yml
│           ├── schema.sql
│           └── mapper/LotteryResultMapper.xml
│
└── frontend/                                   # Vue 3 + Vite 前端
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/index.js
        ├── router/index.js
        └── views/
            ├── Dashboard.vue                   # 数据总览
            ├── Admin.vue                       # 数据管理
            ├── FetchHistory.vue                # 抓取历史
            ├── Analysis.vue                    # 统计分析（全维度图表）
            ├── Trend.vue                       # 趋势分析
            └── Recommend.vue                   # 号码推荐 + 历史 + 命中统计
```

## 技术栈

- **后端**: Java 17, Spring Boot 3, MyBatis, SQLite (可切换 MySQL)
- **前端**: Vue 3, Vite, Vue Router, ECharts, Axios
- **数据源**: 中彩网 (zhcw.com), 体彩网 (sporttery.cn)
