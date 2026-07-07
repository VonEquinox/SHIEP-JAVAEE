package com.hotel.order.service;

import com.hotel.order.dto.CreateOrderDTO;
import com.hotel.order.entity.*;
import com.hotel.order.mapper.OrderItemMapper;
import com.hotel.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    /** VIP 优惠策略：VIP 95 折，SVIP 9 折 */
    private static final Map<String, BigDecimal> DISCOUNT = Map.of(
            "普通", BigDecimal.ONE, "VIP", new BigDecimal("0.95"), "SVIP", new BigDecimal("0.90"));

    @Transactional
    public String create(Integer userId, CreateOrderDTO dto) {
        LocalDate in = dto.getCheckInDate();
        LocalDate out = dto.getCheckOutDate();
        Assert.isTrue(in != null && out != null, "请选择入住和离店日期");
        Assert.isTrue(!in.isBefore(LocalDate.now()), "入住日期不能早于今天");
        Assert.isTrue(out.isAfter(in), "离店日期必须晚于入住日期");
        Assert.notEmpty(dto.getRooms(), "请至少选择一间客房");

        long nights = ChronoUnit.DAYS.between(in, out);
        BigDecimal total = BigDecimal.ZERO;

        // 校验每间房：房型存在、属于该酒店、入住人数合法、库存充足
        Map<Integer, Integer> needByType = new HashMap<>();
        Map<Integer, RoomInfo> infoByType = new HashMap<>();
        for (CreateOrderDTO.RoomReq room : dto.getRooms()) {
            RoomInfo info = infoByType.computeIfAbsent(room.getRoomTypeId(), orderMapper::selectRoomInfo);
            Assert.isTrue(info != null && info.getHotelId().equals(dto.getHotelId()), "房型不存在");
            Assert.isTrue(room.getGuestIds() != null && !room.getGuestIds().isEmpty(), "每间房至少登记一名入住人");
            Assert.isTrue(room.getGuestIds().size() <= info.getCapacity(),
                    info.getTypeName() + "最多入住" + info.getCapacity() + "人");
            needByType.merge(room.getRoomTypeId(), 1, Integer::sum);
            total = total.add(info.getPrice().multiply(BigDecimal.valueOf(nights)));
        }
        needByType.forEach((typeId, need) ->
                Assert.isTrue(need <= infoByType.get(typeId).getAvailableCount(),
                        infoByType.get(typeId).getTypeName() + "仅剩" + infoByType.get(typeId).getAvailableCount() + "间"));

        // 按用户等级折扣计算实付
        String level = orderMapper.selectUserLevel(userId);
        BigDecimal pay = total.multiply(DISCOUNT.getOrDefault(level, BigDecimal.ONE))
                .setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderNo("HB" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")) + userId);
        order.setUserId(userId);
        order.setHotelId(dto.getHotelId());
        order.setCheckInDate(in);
        order.setCheckOutDate(out);
        order.setTotalPrice(total);
        order.setPayPrice(pay);
        order.setStatus("已预订");
        orderMapper.insert(order);

        // 插入明细（触发器自动扣减库存）与每间房的入住人
        for (CreateOrderDTO.RoomReq room : dto.getRooms()) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getOrderId());
            item.setRoomTypeId(room.getRoomTypeId());
            item.setPrice(infoByType.get(room.getRoomTypeId()).getPrice());
            orderItemMapper.insert(item);
            for (Integer guestId : new LinkedHashSet<>(room.getGuestIds())) {
                orderItemMapper.insertItemGuest(item.getItemId(), guestId);
            }
        }
        return order.getOrderNo();
    }

    /** 退订：只有本人的“已预订”订单可退，库存由触发器恢复 */
    public void cancel(Integer userId, Integer orderId) {
        Order order = orderMapper.selectById(orderId);
        Assert.isTrue(order != null && order.getUserId().equals(userId), "订单不存在");
        Assert.isTrue("已预订".equals(order.getStatus()), "只有未入住的订单可以退订");

        order.setStatus("已退订");
        orderMapper.updateStatus(order);
    }

    /** 我的订单（存储过程 sp_user_orders），可再按状态过滤 */
    public List<OrderListItem> myOrders(Integer userId, String status) {
        List<OrderListItem> orders = orderMapper.callUserOrders(userId);
        if (status != null && !status.isEmpty()) {
            orders.removeIf(o -> !status.equals(o.getStatus()));
        }
        return orders;
    }

    public OrderDetailVO detail(Integer userId, String role, Integer orderId) {
        List<OrderDetailRow> rows = orderMapper.selectDetail(orderId);
        Assert.notEmpty(rows, "订单不存在");
        Assert.isTrue("ADMIN".equals(role) || rows.get(0).getUserId().equals(userId), "无权查看该订单");

        OrderDetailVO vo = new OrderDetailVO();
        OrderListItem order = new OrderListItem();
        OrderDetailRow first = rows.get(0);
        order.setOrderId(first.getOrderId());
        order.setOrderNo(first.getOrderNo());
        order.setUsername(first.getUsername());
        order.setHotelName(first.getHotelName());
        order.setCity(first.getCity());
        order.setDistrict(first.getDistrict());
        order.setCheckInDate(first.getCheckInDate());
        order.setCheckOutDate(first.getCheckOutDate());
        order.setTotalPrice(first.getTotalPrice());
        order.setPayPrice(first.getPayPrice());
        order.setStatus(first.getStatus());
        order.setCreatedAt(first.getCreatedAt());
        vo.setOrder(order);

        // 视图行按房间聚合，房间下挂入住人
        Map<Integer, OrderDetailVO.ItemVO> items = new LinkedHashMap<>();
        for (OrderDetailRow row : rows) {
            if (row.getItemId() == null) {
                continue;
            }
            OrderDetailVO.ItemVO item = items.computeIfAbsent(row.getItemId(), id -> {
                OrderDetailVO.ItemVO it = new OrderDetailVO.ItemVO();
                it.setItemId(id);
                it.setTypeName(row.getTypeName());
                it.setRoomPrice(row.getRoomPrice());
                it.setGuests(new ArrayList<>());
                return it;
            });
            if (row.getGuestName() != null) {
                OrderDetailVO.GuestVO g = new OrderDetailVO.GuestVO();
                g.setName(row.getGuestName());
                g.setGender(row.getGender());
                g.setAge(row.getGuestAge());
                item.getGuests().add(g);
            }
        }
        vo.setItems(new ArrayList<>(items.values()));
        return vo;
    }

    // ---------- 后台 ----------

    public List<OrderListItem> adminList(String status) {
        return orderMapper.adminList(status);
    }

    /** 后台流转订单状态；完成时按实付金额 1元=1分 加积分并自动升级 */
    @Transactional
    public void updateStatus(Integer orderId, String status) {
        Order order = orderMapper.selectById(orderId);
        Assert.notNull(order, "订单不存在");

        boolean allowed = ("已入住".equals(status) && "已预订".equals(order.getStatus()))
                || ("已完成".equals(status) && "已入住".equals(order.getStatus()));
        Assert.isTrue(allowed, "不能从「" + order.getStatus() + "」变更为「" + status + "」");

        order.setStatus(status);
        orderMapper.updateStatus(order);
        if ("已完成".equals(status)) {
            orderMapper.addPoints(order.getUserId(), order.getPayPrice().intValue());
        }
    }
}
