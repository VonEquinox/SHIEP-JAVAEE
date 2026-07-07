package com.hotel.order.controller;

import com.hotel.common.Result;
import com.hotel.order.dto.CreateOrderDTO;
import com.hotel.order.entity.OrderDetailVO;
import com.hotel.order.entity.OrderListItem;
import com.hotel.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /** 预订客房，返回订单号 */
    @PostMapping
    public Result<String> create(@RequestHeader("X-User-Id") Integer userId,
                                 @RequestBody CreateOrderDTO dto) {
        return Result.ok(orderService.create(userId, dto));
    }

    /** 退订 */
    @PutMapping("/{orderId}/cancel")
    public Result<Void> cancel(@RequestHeader("X-User-Id") Integer userId,
                               @PathVariable Integer orderId) {
        orderService.cancel(userId, orderId);
        return Result.ok();
    }

    /** 我的订单，可按状态分类查询 */
    @GetMapping("/mine")
    public Result<List<OrderListItem>> mine(@RequestHeader("X-User-Id") Integer userId,
                                            @RequestParam(required = false) String status) {
        return Result.ok(orderService.myOrders(userId, status));
    }

    /** 订单详情（含每间房的入住人） */
    @GetMapping("/{orderId}")
    public Result<OrderDetailVO> detail(@RequestHeader("X-User-Id") Integer userId,
                                        @RequestHeader("X-User-Role") String role,
                                        @PathVariable Integer orderId) {
        return Result.ok(orderService.detail(userId, role, orderId));
    }

    /** 后台：全部订单 */
    @GetMapping("/list")
    public Result<List<OrderListItem>> list(@RequestHeader("X-User-Role") String role,
                                            @RequestParam(required = false) String status) {
        checkAdmin(role);
        return Result.ok(orderService.adminList(status));
    }

    /** 后台：流转订单状态（办理入住 / 完成离店） */
    @PutMapping("/{orderId}/status")
    public Result<Void> updateStatus(@RequestHeader("X-User-Role") String role,
                                     @PathVariable Integer orderId,
                                     @RequestParam String status) {
        checkAdmin(role);
        orderService.updateStatus(orderId, status);
        return Result.ok();
    }

    static void checkAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new IllegalArgumentException("无权限");
        }
    }
}
