# 校园市场系统 · SchoolShop

> 基于 **RuoYi 3.9.2**（Spring Boot 4.1 + Java 17）扩展的校园二手/闲置/商品交易市场系统。
> 覆盖管理员（运营）、商家（发布售卖）、用户（浏览下单）三类角色，交易模式为线上订单 + 线下自提。

## 目录结构

```
SchoolShop/
├── backend/            # 后端（Spring Boot 4.1 多模块）
│   ├── pom.xml
│   ├── ruoyi-admin/    #   启动模块（Controller/配置）
│   ├── ruoyi-common/   #   通用工具/常量/异常/结果封装
│   ├── ruoyi-framework/#   Security/JWT/Redis/WebSocket 配置
│   ├── ruoyi-system/   #   系统用户/角色/菜单/公告
│   ├── ruoyi-quartz/   #   定时任务
│   ├── ruoyi-generator/#   代码生成
│   └── ruoyi-market/   #   业务：分类/商品/店铺/购物车/订单/评价/聊天
├── frontend/           # 前端（三端独立 Vue3 + Vite + TS + Element Plus）
│   ├── campus-admin/   #   管理端（RuoYi-Vue3 衍生，端口 8081）
│   ├── campus-merchant/#   商家端（端口 8082）
│   └── campus-user/    #   用户端（PC 电商风格，端口 8083）
├── ai/                 # AI 分析模块（Python FastAPI + LangChain + LangGraph，端口 8090）
│   └── app/            #   routers / services / workflows（/health + /analysis）
├── docs/               # 文档（开发文档 + 设计/实施计划）
│   └── superpowers/    #   specs（设计） / plans（实施计划）
├── sql/                # 数据库脚本（ruoyi_vue / 业务表 / chat / quartz）
├── scripts/            # 启动/构建脚本（bin/*.bat、ry.bat、ry.sh）
├── tests/              # 测试资产（e2e：Playwright / WS / REST 冒烟脚本）
├── toolchain/          # 本机工具链（JDK17 / Maven / Node / MySQL / Redis）不入库
├── data/               # 运行数据（mysql-data/ 数据库文件）不入库
├── uploadPath/         # 上传文件目录
├── .gitignore
├── LICENSE
└── README.md
```

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 系统权限 | RBAC 用户/角色/菜单，管理端菜单由 sys_menu 持久化驱动 |
| 商家入驻 | 商家申请/店铺管理，商家端行级数据隔离 |
| 商品与分类 | 分类/商品 CRUD，主图上传，上下架 |
| 购物车与订单 | 购物车、按店铺拆单、状态机（待接单→待自提→完成/取消） |
| 订单审查 | 异常订单管理员仲裁 |
| 商品评价 | 评分聚合、重复评价拦截 |
| 在线聊天 | WebSocket 一对一私聊（用户↔商家/用户），消息持久化 + 历史记录，三端接入 |
| 公告推送 | 管理端发布公告 WebSocket 实时推送 + 未读红点 |
| AI 分析 | 独立 Python 服务，/health + /analysis（销售趋势），只读数据库 |

## 快速启动

```bash
# 1. 启动 MySQL（数据在 data/mysql-data）
toolchain\mysql-8.0.29-winx64\bin\mysqld.exe --defaults-file=toolchain\mysql-8.0.29-winx64\my.ini

# 2. 构建并启动后端（8080）
cd backend
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 3. 启动前端三端
cd frontend/campus-user      && npm install && npm run dev   # 8083
cd frontend/campus-merchant  && npm install && npm run dev   # 8082
cd frontend/campus-admin     && npm install && npm run dev   # 8081
```

## 默认账号

| 账号 | 密码 | 角色 | 登录端 |
| --- | --- | --- | --- |
| admin | admin123 | 超级管理员 | 管理端 8081 |
| merchant | admin123 | 商家 | 商家端 8082 |
| ry | admin123 | 普通用户 | 用户端 8083 |

> 登录入口统一为用户端 8083，登录后按角色自动跳转对应端。

## 技术栈

- **后端**：Spring Boot 4.1 / Java 17 / MyBatis(-Plus) / PageHelper / Spring Security + JWT（长短双 Token）/ Redis / MySQL 8 / Spring WebSocket
- **前端**：Vue 3 / Vite / TypeScript / Element Plus / Pinia / Vue Router / Axios
- **AI**：Python / FastAPI / LangChain / LangGraph / SQLAlchemy（只读）

基于若依（RuoYi）开源框架，版权归原作者所有。
