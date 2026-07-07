# 易宿 · 酒店预订管理系统

JavaEE 课程设计：第三方酒店预订平台（类携程）。
单体 Spring Boot 3 + MySQL 8 + Vue3 + Element Plus，前后端分离。需求见 `docs/需求文档.md`。

## 目录结构

```
docs/       需求文档
sql/        建库脚本（按序号执行：表 → 视图 → 触发器 → 存储过程 → 测试数据）
backend/    Maven 多模块：common / app(8080，单体后端)
frontend/   Vue3 + Element Plus（Vite，5173 端口，/api 代理到单体后端）
.local/     本地日志、截图等运行文件，不入库
```

## 启动步骤

一键启动：

```bash
./start.sh
```

脚本会导入 `sql/0*.sql`、打包后端、安装前端依赖并启动前后端；按 `Ctrl+C` 会停止脚本启动的服务。若只想复用现有数据库，可执行：

```bash
RUN_SQL=0 ./start.sh
```

手动启动：

```bash
# 1. 建库（MySQL root/kk112233）
cd sql && for f in 0*.sql; do mysql -uroot -pkk112233 < "$f"; done

# 2. 打包并启动单体后端
cd backend && mvn package -DskipTests
java -jar app/target/app-1.0.0.jar

# 3. 启动前端
cd frontend && npm install && npm run dev
# 打开 http://localhost:5173
```

## 测试账号（密码均为 123456）

| 账号 | 角色 | 说明 |
|---|---|---|
| admin | 管理员 | 后台管理、统计报表 |
| zhangsan | 用户（普通） | 300 积分 |
| lisi | 用户（VIP） | 预订 95 折 |
| wangwu | 用户（SVIP） | 预订 9 折 |

## 作业要求对照

- **视图**：`v_hotel_room`（酒店客房详情）、`v_order_detail`（订单明细展开，报表明细复用）
- **触发器**：`trg_order_item_insert`（预订扣库存）、`trg_order_cancel`（退订恢复库存）
- **存储过程**：`sp_user_orders`（带参查用户订单）、`sp_occupancy_rate`（入住率）、
  `sp_booking_stats`（按城市/区域/酒店/价格统计）、`sp_guest_stats`（入住人画像统计）
- **完整性**：主外键、缺省（性别/日期/货币列）、非空、CHECK（日期先后、身份证 18 位、评分 1~5）、索引
- **积分规则**：订单完成按实付 1 元 = 1 分；≥1000 升 VIP（95 折），≥5000 升 SVIP（9 折）
- **简单档特性**：单体 Spring Boot、Controller-Service-Mapper 三层、BCrypt 密码加密、MyBatis 参数绑定防 SQL 注入、后台房型表格多选批量删除。
