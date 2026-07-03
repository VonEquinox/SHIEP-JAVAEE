-- =====================================================
-- 03_triggers.sql  触发器（对应需求文档第 6 节）
-- =====================================================
USE hotel_booking;

-- 触发器一：客房预定处理
-- 每预订一间房（插入一条订单明细），该房型可预订数量 -1
DROP TRIGGER IF EXISTS trg_order_item_insert;
DELIMITER $$
CREATE TRIGGER trg_order_item_insert
    AFTER INSERT
    ON t_order_item
    FOR EACH ROW
BEGIN
    UPDATE t_room_type
    SET available_count = available_count - 1
    WHERE room_type_id = NEW.room_type_id;
END$$
DELIMITER ;

-- 触发器二：退订处理
-- 订单从“已预订”改为“已退订”时，把该订单占用的各房型数量加回去
DROP TRIGGER IF EXISTS trg_order_cancel;
DELIMITER $$
CREATE TRIGGER trg_order_cancel
    AFTER UPDATE
    ON t_order
    FOR EACH ROW
BEGIN
    IF OLD.status = '已预订' AND NEW.status = '已退订' THEN
        UPDATE t_room_type rt
            JOIN (SELECT room_type_id, COUNT(*) AS cnt
                  FROM t_order_item
                  WHERE order_id = NEW.order_id
                  GROUP BY room_type_id) x ON x.room_type_id = rt.room_type_id
        SET rt.available_count = rt.available_count + x.cnt;
    END IF;
END$$
DELIMITER ;
