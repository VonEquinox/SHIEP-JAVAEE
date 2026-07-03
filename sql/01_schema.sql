-- =====================================================
-- 01_schema.sql  建库建表 + 完整性约束 + 索引
-- 对应需求文档 5.2 / 5.3 / 5.4
-- =====================================================

DROP DATABASE IF EXISTS hotel_booking;
CREATE DATABASE hotel_booking DEFAULT CHARACTER SET utf8mb4;
USE hotel_booking;

-- 用户表
CREATE TABLE t_user (
    user_id    INT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(20) NOT NULL UNIQUE,
    password   VARCHAR(64) NOT NULL,
    phone      CHAR(11)    NOT NULL,
    id_card    CHAR(18),
    points     INT         DEFAULT 0,                -- 积分，1 元 = 1 分
    level      VARCHAR(10) DEFAULT '普通',           -- 普通 / VIP / SVIP
    role       VARCHAR(10) DEFAULT '用户',           -- 用户 / 管理员
    created_at DATETIME    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_user_idcard CHECK (id_card IS NULL OR CHAR_LENGTH(id_card) = 18),  -- “规则”：身份证必须 18 位
    CONSTRAINT ck_user_points CHECK (points >= 0),
    CONSTRAINT ck_user_level  CHECK (level IN ('普通', 'VIP', 'SVIP')),
    CONSTRAINT ck_user_role   CHECK (role IN ('用户', '管理员'))
);

-- 常用入住人表（属于某个用户，画像字段用于统计）
CREATE TABLE t_guest (
    guest_id     INT PRIMARY KEY AUTO_INCREMENT,
    user_id      INT NOT NULL,
    name         VARCHAR(20) NOT NULL,
    id_card      CHAR(18),
    gender       CHAR(1)     DEFAULT '男',           -- 缺省约束示例
    birth_date   DATE,
    occupation   VARCHAR(20),                        -- 职业
    education    VARCHAR(10),                        -- 受教育程度
    income_level VARCHAR(10),                        -- 收入状况
    CONSTRAINT fk_guest_user   FOREIGN KEY (user_id) REFERENCES t_user (user_id),
    CONSTRAINT ck_guest_idcard CHECK (id_card IS NULL OR CHAR_LENGTH(id_card) = 18),
    CONSTRAINT ck_guest_gender CHECK (gender IN ('男', '女'))
);

-- 酒店表
CREATE TABLE t_hotel (
    hotel_id   INT PRIMARY KEY AUTO_INCREMENT,
    hotel_name VARCHAR(50) NOT NULL,
    city       VARCHAR(20) NOT NULL,
    district   VARCHAR(20) NOT NULL,                 -- 区域
    address    VARCHAR(100),
    star       TINYINT,
    phone      VARCHAR(20),
    CONSTRAINT ck_hotel_star CHECK (star BETWEEN 1 AND 5)
);

-- 房型表（客房管理到“房型 + 数量”粒度）
CREATE TABLE t_room_type (
    room_type_id    INT PRIMARY KEY AUTO_INCREMENT,
    hotel_id        INT NOT NULL,
    type_name       VARCHAR(20)   NOT NULL,          -- 非空约束：客房类型名
    price           DECIMAL(10,2) DEFAULT 0,         -- 货币列统一缺省 0
    capacity        TINYINT       NOT NULL,          -- 可住人数（标间=2）
    total_count     INT           NOT NULL,          -- 客房总数
    available_count INT           NOT NULL,          -- 可预订数量（触发器维护）
    CONSTRAINT fk_room_hotel FOREIGN KEY (hotel_id) REFERENCES t_hotel (hotel_id),
    CONSTRAINT ck_room_price CHECK (price >= 0),
    CONSTRAINT ck_room_cap   CHECK (capacity >= 1),
    CONSTRAINT ck_room_avail CHECK (available_count BETWEEN 0 AND total_count)
);

-- 订单表
CREATE TABLE t_order (
    order_id       INT PRIMARY KEY AUTO_INCREMENT,
    order_no       VARCHAR(32)   NOT NULL UNIQUE,
    user_id        INT NOT NULL,
    hotel_id       INT NOT NULL,
    check_in_date  DATE NOT NULL DEFAULT (CURRENT_DATE),  -- 缺省：入住日期默认当天（对应 GETDATE()）
    check_out_date DATE NOT NULL,
    total_price    DECIMAL(10,2) DEFAULT 0,          -- 原价
    pay_price      DECIMAL(10,2) DEFAULT 0,          -- 折后实付
    status         VARCHAR(10)   DEFAULT '已预订',   -- 已预订 / 已入住 / 已完成 / 已退订
    created_at     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user  FOREIGN KEY (user_id)  REFERENCES t_user (user_id),
    CONSTRAINT fk_order_hotel FOREIGN KEY (hotel_id) REFERENCES t_hotel (hotel_id),
    CONSTRAINT ck_order_date  CHECK (check_out_date > check_in_date),   -- 离店必须晚于入住
    CONSTRAINT ck_order_status CHECK (status IN ('已预订', '已入住', '已完成', '已退订'))
);

-- 订单房间明细表（一行 = 一间房）
CREATE TABLE t_order_item (
    item_id      INT PRIMARY KEY AUTO_INCREMENT,
    order_id     INT NOT NULL,
    room_type_id INT NOT NULL,
    price        DECIMAL(10,2) DEFAULT 0,            -- 下单时房型单价快照
    CONSTRAINT fk_item_order FOREIGN KEY (order_id)     REFERENCES t_order (order_id),
    CONSTRAINT fk_item_room  FOREIGN KEY (room_type_id) REFERENCES t_room_type (room_type_id)
);

-- 房间-入住人关联表（一间房可住多人）
CREATE TABLE t_item_guest (
    item_id  INT NOT NULL,
    guest_id INT NOT NULL,
    PRIMARY KEY (item_id, guest_id),
    CONSTRAINT fk_ig_item  FOREIGN KEY (item_id)  REFERENCES t_order_item (item_id),
    CONSTRAINT fk_ig_guest FOREIGN KEY (guest_id) REFERENCES t_guest (guest_id)
);

-- 评价表（一单一评）
CREATE TABLE t_review (
    review_id  INT PRIMARY KEY AUTO_INCREMENT,
    order_id   INT NOT NULL UNIQUE,
    user_id    INT NOT NULL,
    score      TINYINT NOT NULL,
    content    VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES t_order (order_id),
    CONSTRAINT fk_review_user  FOREIGN KEY (user_id)  REFERENCES t_user (user_id),
    CONSTRAINT ck_review_score CHECK (score BETWEEN 1 AND 5)
);

-- 索引设计（对应需求文档 5.4，服务于搜索与统计查询）
CREATE INDEX idx_hotel_city_district ON t_hotel (city, district);   -- 按城市/区域检索
CREATE INDEX idx_hotel_name          ON t_hotel (hotel_name);       -- 按酒店名前缀模糊检索
CREATE INDEX idx_room_hotel          ON t_room_type (hotel_id);     -- 酒店详情列房型
CREATE INDEX idx_order_user_status   ON t_order (user_id, status);  -- 用户按状态查订单
CREATE INDEX idx_order_checkin       ON t_order (check_in_date);    -- 按时间段统计
