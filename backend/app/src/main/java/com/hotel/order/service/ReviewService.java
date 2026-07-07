package com.hotel.order.service;

import com.hotel.order.entity.Order;
import com.hotel.order.entity.Review;
import com.hotel.order.mapper.OrderMapper;
import com.hotel.order.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;

    /** 只有本人已完成的订单可以评价，一单一评 */
    public void add(Integer userId, Review review) {
        Assert.isTrue(review.getScore() != null && review.getScore() >= 1 && review.getScore() <= 5,
                "评分必须为1~5星");
        Order order = orderMapper.selectById(review.getOrderId());
        Assert.isTrue(order != null && order.getUserId().equals(userId), "订单不存在");
        Assert.isTrue("已完成".equals(order.getStatus()), "只有已完成的订单可以评价");
        long exists = reviewMapper.countByOrderId(review.getOrderId());
        Assert.isTrue(exists == 0, "该订单已评价过");

        review.setReviewId(null);
        review.setUserId(userId);
        review.setCreatedAt(null);
        reviewMapper.insert(review);
    }

    public List<Map<String, Object>> listByHotel(Integer hotelId) {
        return reviewMapper.listByHotel(hotelId);
    }
}
