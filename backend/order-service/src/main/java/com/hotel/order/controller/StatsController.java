package com.hotel.order.controller;

import com.hotel.common.Result;
import com.hotel.order.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** 汇总统计报表（后台），返回 {summary: 汇总, detail: 明细} */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 按 city/district/hotel/price 统计预订情况 */
    @GetMapping("/booking")
    public Result<Map<String, Object>> booking(@RequestHeader("X-User-Role") String role,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                               @RequestParam(defaultValue = "city") String group) {
        OrderController.checkAdmin(role);
        return Result.ok(statsService.booking(start, end, group));
    }

    /** 按入住人 age/gender/occupation/education/income 统计偏好 */
    @GetMapping("/guest")
    public Result<Map<String, Object>> guest(@RequestHeader("X-User-Role") String role,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                             @RequestParam(defaultValue = "age") String dim) {
        OrderController.checkAdmin(role);
        return Result.ok(statsService.guest(start, end, dim));
    }

    /** 各类客房入住率 */
    @GetMapping("/occupancy")
    public Result<Map<String, Object>> occupancy(@RequestHeader("X-User-Role") String role,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        OrderController.checkAdmin(role);
        return Result.ok(statsService.occupancy(start, end));
    }
}
