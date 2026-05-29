package com.luqiang.seckill.service;

import com.luqiang.seckill.entity.Goods;
import java.util.List;
import java.util.Map;

public interface GoodsService {
    List<Goods> listGoods();

    Map<String, Object> getGoodsDetail(Long goodsId);
}