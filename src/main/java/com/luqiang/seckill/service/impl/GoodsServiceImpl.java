package com.luqiang.seckill.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luqiang.seckill.common.CacheConstants;
import com.luqiang.seckill.common.LocalStockCache;
import com.luqiang.seckill.common.RedisUtil;
import com.luqiang.seckill.entity.Goods;
import com.luqiang.seckill.repository.GoodsRepository;
import com.luqiang.seckill.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GoodsServiceImpl implements GoodsService {
    private static final Logger log = LoggerFactory.getLogger(GoodsServiceImpl.class);

    private final RedisUtil redisUtil;
    private final GoodsRepository goodsRepository;
    private final ObjectMapper objectMapper;
    private final LocalStockCache localStockCache;

    public GoodsServiceImpl(RedisUtil redisUtil,
                            GoodsRepository goodsRepository,
                            ObjectMapper objectMapper,
                            LocalStockCache localStockCache) {
        this.redisUtil = redisUtil;
        this.goodsRepository = goodsRepository;
        this.objectMapper = objectMapper;
        this.localStockCache = localStockCache;
    }

    @Override
    public List<Goods> listGoods() {

        try {
            String cache = redisUtil.get(CacheConstants.GOODS_LIST_KEY);

            if (cache != null) {
                if (CacheConstants.EMPTY_CACHE_MARKER.equals(cache)) {
                    return Collections.emptyList();
                }
                List<Goods> goodsList = Arrays.asList(objectMapper.readValue(cache, Goods[].class));
                refreshStock(goodsList);
                return goodsList;
            }

            Boolean locked = redisUtil.setIfAbsent(CacheConstants.GOODS_LIST_LOCK_KEY, "1", 10);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    String cacheAgain = redisUtil.get(CacheConstants.GOODS_LIST_KEY);
                    if (cacheAgain != null) {
                        if (CacheConstants.EMPTY_CACHE_MARKER.equals(cacheAgain)) {
                            return Collections.emptyList();
                        }
                        List<Goods> goodsList = Arrays.asList(objectMapper.readValue(cacheAgain, Goods[].class));
                        refreshStock(goodsList);
                        return goodsList;
                    }

                    List<Goods> goodsList = goodsRepository.findAll();
                    if (goodsList == null || goodsList.isEmpty()) {
                        redisUtil.set(
                                CacheConstants.GOODS_LIST_KEY,
                                CacheConstants.EMPTY_CACHE_MARKER,
                                CacheConstants.GOODS_LIST_EMPTY_TTL_SECONDS
                        );
                        return Collections.emptyList();
                    }

                    refreshStock(goodsList);

                    int ttl = CacheConstants.GOODS_LIST_TTL_SECONDS + ThreadLocalRandom.current()
                            .nextInt(CacheConstants.GOODS_LIST_TTL_RANDOM_BOUND_SECONDS + 1);
                    redisUtil.set(CacheConstants.GOODS_LIST_KEY, objectMapper.writeValueAsString(goodsList), ttl);

                    return goodsList;
                } finally {
                    redisUtil.delete(CacheConstants.GOODS_LIST_LOCK_KEY);
                }
            }

            Thread.sleep(50L);
            String cacheRetry = redisUtil.get(CacheConstants.GOODS_LIST_KEY);
            if (cacheRetry != null) {
                if (CacheConstants.EMPTY_CACHE_MARKER.equals(cacheRetry)) {
                    return Collections.emptyList();
                }
                List<Goods> goodsList = Arrays.asList(objectMapper.readValue(cacheRetry, Goods[].class));
                refreshStock(goodsList);
                return goodsList;
            }

            List<Goods> fallbackList = goodsRepository.findAll();
            if (fallbackList == null || fallbackList.isEmpty()) {
                return Collections.emptyList();
            }
            refreshStock(fallbackList);
            return fallbackList;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询商品被中断", e);
        } catch (Exception e) {
            log.error("查询商品失败", e);
            throw new RuntimeException("查询商品失败", e);
        }
    }

    @Override
    public Map<String, Object> getGoodsDetail(Long goodsId) {
        Goods goods = goodsRepository.findById(goodsId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        int currentStock = calcCurrentStock(goodsId);
        int sold = goods.getStock() - currentStock;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", goods.getId());
        result.put("name", goods.getName());
        result.put("price", goods.getPrice());
        result.put("initialStock", goods.getStock());
        result.put("currentStock", Math.max(0, currentStock));
        result.put("sold", Math.max(0, sold));
        return result;
    }

    /**
     * 汇总 Redis 分段库存 + 本地缓存库存，计算实时剩余数量。
     */
    private int calcCurrentStock(Long goodsId) {
        int redisStock = 0;
        int localStock = 0;
        for (int i = 0; i < CacheConstants.STOCK_SEGMENTS; i++) {
            String v = redisUtil.get(CacheConstants.stockKey(goodsId, i));
            if (v != null) {
                redisStock += Integer.parseInt(v);
            }
            localStock += localStockCache.localRemaining(CacheConstants.stockKey(goodsId, i));
        }
        return Math.max(0, redisStock + localStock);
    }

    /**
     * 为商品列表刷新实时库存。
     */
    private void refreshStock(List<Goods> goodsList) {
        for (Goods goods : goodsList) {
            goods.setStock(calcCurrentStock(goods.getId()));
        }
    }
}
