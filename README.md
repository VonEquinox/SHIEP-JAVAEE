# 易宿 · 酒店预订管理系统

JavaEE 课程设计：第三方酒店预订平台（类携程）。
Spring Boot 3 微服务（Nacos）+ MySQL 8 + Vue3 + Element Plus。需求见 `docs/需求文档.md`。

## 目录结构

```
docs/       需求文档
sql/        建库脚本（按序号执行：表 → 视图 → 触发器 → 存储过程 → 测试数据）
backend/    Maven 多模块：common / gateway(8080) / user-service(8081)
            / hotel-service(8082) / order-service(8083)
frontend/   Vue3 + Element Plus（Vite，5173 端口，/api 代理到网关）
.local/     本地运行环境（Nacos、日志），不入库
```

## 启动步骤

```bash
# 1. 建库（MySQL root/kk112233）
cd sql && for f in 0*.sql; do mysql -uroot -pkk112233 < "$f"; done

# 2. 启动 Nacos（单机模式，8848）
bash .local/nacos/bin/startup.sh -m standalone

# 3. 打包并启动四个后端服务
cd backend && mvn package -DskipTests
for svc in gateway user-service hotel-service order-service; do
  java -jar $svc/target/$svc-1.0.0.jar &
done

# 4. 启动前端
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
