-- =====================================================
-- 02_views.sql  视图（对应需求文档第 6 节）
-- =====================================================
USE hotel_booking;

-- 视图一：客人选择客房处理
-- 按 城市/区域/酒店 查询时，展示酒店的所有客房详细信息
CREATE OR REPLACE VIEW v_hotel_room AS
SELECT h.hotel_id,
       h.hotel_name,
       h.city,
       h.district,
       h.address,
       h.star,
       rt.room_type_id,
       rt.type_name,
       rt.price,
       rt.capacity,
       rt.total_count,
       rt.available_count
FROM t_hotel h
         JOIN t_room_type rt ON rt.hotel_id = h.hotel_id;

-- 视图二：订单明细展开
-- 订单 + 酒店 + 房间明细 + 入住人画像，供订单查询与统计报表的“明细”部分复用
CREATE OR REPLACE VIEW v_order_detail AS
SELECT o.order_id,
       o.order_no,
       o.user_id,
       u.username,
       o.status,
       o.check_in_date,
       o.check_out_date,
       o.total_price,
       o.pay_price,
       o.created_at,
       h.hotel_id,
       h.hotel_name,
       h.city,
       h.district,
       oi.item_id,
       rt.type_name,
       oi.price                                          AS room_price,
       g.name                                            AS guest_name,
       g.gender,
       TIMESTAMPDIFF(YEAR, g.birth_date, CURDATE())      AS guest_age,
       g.occupation,
       g.education,
       g.income_level
FROM t_order o
         JOIN t_user u        ON u.user_id = o.user_id
         JOIN t_hotel h       ON h.hotel_id = o.hotel_id
         LEFT JOIN t_order_item oi ON oi.order_id = o.order_id
         LEFT JOIN t_room_type rt  ON rt.room_type_id = oi.room_type_id
         LEFT JOIN t_item_guest ig ON ig.item_id = oi.item_id
         LEFT JOIN t_guest g       ON g.guest_id = ig.guest_id;
