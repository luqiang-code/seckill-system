package com.luqiang.seckill.service;

import com.luqiang.seckill.common.ApiResponse;
import com.luqiang.seckill.entity.OrderInfo;

import java.util.List;
import java.util.Map;

public interface SeckillService {
    ApiResponse<Void> executeSeckill(Long goodsId, String userId);

    ApiResponse<Integer> getStock(Long goodsId);

    ApiResponse<OrderInfo> getResult(Long goodsId, String userId);

    ApiResponse<List<Map<String, Object>>> getRecentOrders(Long goodsId, int limit);
}