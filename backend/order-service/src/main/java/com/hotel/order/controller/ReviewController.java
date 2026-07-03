package com.hotel.order.controller;

import com.hotel.common.Result;
import com.hotel.order.entity.Review;
import com.hotel.order.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 评价已完成的订单 */
    @PostMapping
    public Result<Void> add(@RequestHeader("X-User-Id") Integer userId, @RequestBody Review review) {
        reviewService.add(userId, review);
        return Result.ok();
    }

    /** 酒店评价列表（游客可看） */
    @GetMapping("/hotel/{hotelId}")
    public Result<List<Map<String, Object>>> listByHotel(@PathVariable Integer hotelId) {
        return Result.ok(reviewService.listByHotel(hotelId));
    }
}
