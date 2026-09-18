# AI 模块（aimodule）

独立 Python 服务，基于 **FastAPI + LangChain + LangGraph**，本期提供**可运行的骨架**，用于后续给管理端提供数据分析。

> 定位：**独立进程 + 独立端口**，只读访问 MySQL，与 SpringBoot 后端（8080）解耦。本期**不接真实大模型**，`/analysis` 以 LangGraph 工作流跑通一条**销售趋势 SQL 分析**作为占位实现，配置了 LLM Key 后可无缝接入 LLM 解读。

---

## 1. 目录结构

```
aimodule/
├── app/
│   ├── __init__.py
│   ├── main.py            # FastAPI 入口：装配 app、生命周期、路由注册
│   ├── config.py          # 配置（pydantic-settings：端口 / DB / 内部令牌 / LLM）
│   ├── db.py              # 只读数据库引擎与会话（SQLAlchemy 2.x + pymysql）
│   ├── models.py          # 分析所使用的只读表模型（映射视图/表）
│   ├── schemas.py         # Pydantic DTO：analysis 入参/出参契约
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── health.py      # GET /health
│   │   └── analysis.py    # POST /analysis
│   ├── services/
│   │   ├── __init__.py
│   │   └── analyzer.py    # 对外分析服务：将请求参数送入 LangGraph 工作流
│   └── workflows/
│       ├── __init__.py
│       └── sales_trend.py # LangGraph 工作流：销售趋势分析（编译后的图为可运行骨架）
├── requirements.txt       # 依赖清单
├── .env.example           # 环境变量模板
├── run.py                 # 启动入口：uvicorn 命令行（或直接 uvicorn app.main:app）
└── README.md
```

## 2. 对接契约（冻结）

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| 健康检查 | `GET /health` | 返回服务存活、版本、DB 连通性 |
| 数据分析 | `POST /analysis` | 入参：指标 / 时间范围 / 维度；出参：结构化结果 JSON |

### `POST /analysis` 入参示例

```json
{
  "metric": "sales_trend",
  "start_date": "2026-08-01",
  "end_date": "2026-08-23",
  "group_by": "day",
  "options": {}
}
```

### `POST /analysis` 出参（骨架阶段统一结构）

```json
{
  "code": 200,
  "metric": "sales_trend",
  "data": { "points": [] },
  "ai_summary": null
}
```

> `ai_summary`：当前为 `null`。配置了 LLM Key（`LLM_API_KEY`）并启用 `ANALYSIS_ENABLE_LLM=true` 时，工作流会在 SQL 结果基础上追加一段自然语言解读。

---

## 3. 快速启动

```bash
cd aimodule
python -m venv .venv
# Windows
.venv\Scripts\activate
# macOS / Linux
source .venv/bin/activate

pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

验证：

```bash
curl http://localhost:8090/health
curl -X POST http://localhost:8090/analysis \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <AI_INTERNAL_TOKEN>" \
  -d '{"metric":"sales_trend","end_date":"2026-08-23","group_by":"day"}'
```

## 4. 架构说明

```
调用方(后端) --HTTP--> FastAPI(/analysis) --> Analyzer.service
        --> LangGraph(StateGraph) --> SQL 只读查询(MySQL) --> 结构化结果 --> 返回
                                          │
                                          └--(可选)配置 LLM --> AI 解读
```

- **LangGraph 的价值**：把「数据查询 → 清洗/聚合 → 可选 LLM 解读 → 输出」编排成有状态工作流，便于后续扩展多步分析或条件分支，本期先落地一条可运行链路。
- **读库隔离**：`db.py` 使用只读账号，仅映射分析所需视图；不建表、不写库。

## 5. 安全

- `/analysis` 需要内部令牌 `X-AI-Token` 或 `Authorization: Bearer`，令牌由环境变量 `AI_INTERNAL_TOKEN` 配置，后端调用时携带同一令牌。
- 服务默认只监听本机，不做公网暴露；如需暴露交由网关/Nginx 控制。

---

> 本文档随骨架交付，对应「开发文档.md」§9 AI 模块设计（预留）与任务 M8。