-- =====================================================
-- 04_procedures.sql  存储过程（对应需求文档第 6 节）
-- 时间段参数均为闭区间 [p_start, p_end]，按入住日期筛选
-- =====================================================
USE hotel_booking;

-- 过程一：用户信息查询处理（带输入参数）
-- 按用户查询其所有订单信息
DROP PROCEDURE IF EXISTS sp_user_orders;
DELIMITER $$
CREATE PROCEDURE sp_user_orders(IN p_user_id INT)
BEGIN
    SELECT o.order_id,
           o.order_no,
           h.hotel_name,
           h.city,
           h.district,
           o.check_in_date,
           o.check_out_date,
           o.total_price,
           o.pay_price,
           o.status,
           o.created_at
    FROM t_order o
             JOIN t_hotel h ON h.hotel_id = o.hotel_id
    WHERE o.user_id = p_user_id
    ORDER BY o.created_at DESC;
END$$
DELIMITER ;

-- 过程二：统计一段时间内各类客房的入住率
-- 入住率 = 时间段内已售间夜数 / (房型总间数 × 时间段天数)，退订订单不计
DROP PROCEDURE IF EXISTS sp_occupancy_rate;
DELIMITER $$
CREATE PROCEDURE sp_occupancy_rate(IN p_start DATE, IN p_end DATE)
BEGIN
    DECLARE v_days INT DEFAULT DATEDIFF(p_end, p_start) + 1;

    SELECT h.hotel_name,
           h.city,
           rt.type_name,
           rt.total_count,
           -- 每间房在统计期内的实际过夜数（与统计期取交集）
           COALESCE(SUM(GREATEST(0, DATEDIFF(
                   LEAST(o.check_out_date, DATE_ADD(p_end, INTERVAL 1 DAY)),
                   GREATEST(o.check_in_date, p_start)))), 0)              AS sold_nights,
           rt.total_count * v_days                                        AS total_nights,
           ROUND(COALESCE(SUM(GREATEST(0, DATEDIFF(
                   LEAST(o.check_out_date, DATE_ADD(p_end, INTERVAL 1 DAY)),
                   GREATEST(o.check_in_date, p_start)))), 0)
                     / (rt.total_count * v_days) * 100, 2)                AS occupancy_rate
    FROM t_room_type rt
             JOIN t_hotel h ON h.hotel_id = rt.hotel_id
             LEFT JOIN t_order_item oi ON oi.room_type_id = rt.room_type_id
             LEFT JOIN t_order o ON o.order_id = oi.order_id
        AND o.status <> '已退订'
        AND o.check_in_date <= p_end
        AND o.check_out_date > p_start
    GROUP BY rt.room_type_id, h.hotel_name, h.city, rt.type_name, rt.total_count
    ORDER BY occupancy_rate DESC;
END$$
DELIMITER ;

-- 过程三：按 城市/区域/酒店/价格区间 统计一段时间内客房预订情况
-- p_group 取值：city / district / hotel / price
DROP PROCEDURE IF EXISTS sp_booking_stats;
DELIMITER $$
CREATE PROCEDURE sp_booking_stats(IN p_start DATE, IN p_end DATE, IN p_group VARCHAR(10))
BEGIN
    SELECT CASE p_group
               WHEN 'city' THEN h.city
               WHEN 'district' THEN CONCAT(h.city, '-', h.district)
               WHEN 'hotel' THEN h.hotel_name
               ELSE CASE
                        WHEN oi.price < 200 THEN '200元以下'
                        WHEN oi.price < 400 THEN '200-399元'
                        WHEN oi.price < 600 THEN '400-599元'
                        ELSE '600元及以上' END
               END                                                    AS group_name,
           COUNT(DISTINCT o.order_id)                                 AS order_count,
           COUNT(oi.item_id)                                          AS room_count,
           SUM(DATEDIFF(o.check_out_date, o.check_in_date))           AS night_count,
           SUM(oi.price * DATEDIFF(o.check_out_date, o.check_in_date)) AS amount
    FROM t_order o
             JOIN t_hotel h ON h.hotel_id = o.hotel_id
             JOIN t_order_item oi ON oi.order_id = o.order_id
    WHERE o.status <> '已退订'
      AND o.check_in_date BETWEEN p_start AND p_end
    GROUP BY group_name
    ORDER BY amount DESC;
END$$
DELIMITER ;

-- 过程四：按入住人画像统计客房预订偏好
-- p_dim 取值：age / gender / occupation / education / income
-- 结果 = 每个画像分组 × 房型 的预订间数，用于分析不同客群喜好
DROP PROCEDURE IF EXISTS sp_guest_stats;
DELIMITER $$
CREATE PROCEDURE sp_guest_stats(IN p_start DATE, IN p_end DATE, IN p_dim VARCHAR(10))
BEGIN
    SELECT CASE p_dim
               WHEN 'gender' THEN g.gender
               WHEN 'occupation' THEN COALESCE(g.occupation, '未知')
               WHEN 'education' THEN COALESCE(g.education, '未知')
               WHEN 'income' THEN COALESCE(g.income_level, '未知')
               ELSE CASE
                        WHEN TIMESTAMPDIFF(YEAR, g.birth_date, CURDATE()) < 18 THEN '18岁以下'
                        WHEN TIMESTAMPDIFF(YEAR, g.birth_date, CURDATE()) <= 30 THEN '18-30岁'
                        WHEN TIMESTAMPDIFF(YEAR, g.birth_date, CURDATE()) <= 45 THEN '31-45岁'
                        WHEN TIMESTAMPDIFF(YEAR, g.birth_date, CURDATE()) <= 60 THEN '46-60岁'
                        ELSE '60岁以上' END
               END                       AS dim_value,
           rt.type_name,
           COUNT(oi.item_id)             AS room_count
    FROM t_order o
             JOIN t_order_item oi ON oi.order_id = o.order_id
             JOIN t_room_type rt ON rt.room_type_id = oi.room_type_id
             JOIN t_item_guest ig ON ig.item_id = oi.item_id
             JOIN t_guest g ON g.guest_id = ig.guest_id
    WHERE o.status <> '已退订'
      AND o.check_in_date BETWEEN p_start AND p_end
    GROUP BY dim_value, rt.type_name
    ORDER BY dim_value, room_count DESC;
END$$
DELIMITER ;
