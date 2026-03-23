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

### 一键生成演示数据

后端启动后：
```bash
# 拉取演示数据（100期）
curl -X POST "http://localhost:8080/api/lottery/fetch?count=100&useDemo=true"

# 查看状态
curl http://localhost:8080/api/lottery/status

# 统计分析
curl http://localhost:8080/api/lottery/analyze?type=ssq
```

或直接在前端页面点击「📡 拉取数据」/「🎲 演示数据」按钮。

## API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/lottery/status` | 数据统计状态 |
| GET | `/api/lottery/results?type=ssq&limit=20` | 查询开奖记录 |
| GET | `/api/lottery/latest?type=ssq` | 最新一期期号 |
| POST | `/api/lottery/fetch?count=30&useDemo=true` | 拉取全部彩种 |
| POST | `/api/lottery/fetch/{type}?count=30` | 拉取指定彩种 |
| GET | `/api/lottery/analyze?type=ssq` | 统计分析 |
| GET | `/api/lottery/trend?type=ssq&n=30` | 趋势分析 |

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

3. 修改 `schema.sql` 中的建表语句：
   ```sql
   CREATE TABLE IF NOT EXISTS lottery_result (
       id          INT PRIMARY KEY AUTO_INCREMENT,
       lottery_type VARCHAR(20) NOT NULL,
       ...
   );
   ```

## 统计功能

- **频率分布** — 每个号码出现次数（柱状图）
- **遗漏值** — 每个号码已多少期未出现
- **热号/冷号** — TOP10 / 末10
- **和值统计** — 平均、范围
- **奇偶比/大小比** — 双色球特有
- **三区分布** — 双色球分区统计
- **位置分析** — 3D/排列系列每位独立统计
- **趋势图** — 和值走势、奇偶走势、分区走势（折线图）

## 项目结构

```
lottery-java/
├── backend/                          # Spring Boot 3 后端
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/lottery/
│       │   ├── LotteryApplication.java
│       │   ├── config/CorsConfig.java
│       │   ├── controller/LotteryController.java
│       │   ├── entity/{LotteryResult,LotteryType}.java
│       │   ├── mapper/LotteryResultMapper.java
│       │   ├── service/
│       │   │   ├── LotteryResultService.java
│       │   │   ├── FetchService.java
│       │   │   ├── AnalysisService.java
│       │   │   └── impl/FetchServiceImpl.java
│       │   └── task/DailyFetchTask.java
│       └── resources/
│           ├── application.yml
│           ├── schema.sql
│           └── mapper/LotteryResultMapper.xml
│
└── frontend/                         # Vue 3 前端
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/index.js
        ├── router/index.js
        └── views/
            ├── Dashboard.vue         # 数据总览 + 拉取
            ├── Analysis.vue          # 统计分析 + 图表
            └── Trend.vue             # 趋势分析
```
