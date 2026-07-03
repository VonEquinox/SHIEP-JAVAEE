package com.hotel.order.service;

import com.hotel.order.entity.OrderDetailRow;
import com.hotel.order.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 汇总统计：调用存储过程得到汇总，配合视图明细，报表 = 汇总 + 明细 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsMapper statsMapper;

    public Map<String, Object> booking(LocalDate start, LocalDate end, String group) {
        check(start, end);
        return Map.of("summary", statsMapper.bookingStats(start, end, group),
                "detail", statsMapper.detailBetween(start, end));
    }

    public Map<String, Object> guest(LocalDate start, LocalDate end, String dim) {
        check(start, end);
        return Map.of("summary", statsMapper.guestStats(start, end, dim),
                "detail", statsMapper.detailBetween(start, end));
    }

    public Map<String, Object> occupancy(LocalDate start, LocalDate end) {
        check(start, end);
        List<Map<String, Object>> summary = statsMapper.occupancy(start, end);
        List<OrderDetailRow> detail = statsMapper.detailBetween(start, end);
        return Map.of("summary", summary, "detail", detail);
    }

    private void check(LocalDate start, LocalDate end) {
        Assert.isTrue(start != null && end != null, "请选择统计时间段");
        Assert.isTrue(!end.isBefore(start), "结束日期不能早于开始日期");
    }
}
