-- ============================================
-- SQL 基础练习：goods 表
-- ============================================

-- 素材表结构
-- goods(id BIGINT, name VARCHAR, price DECIMAL, stock INT)


-- ========== 1. CASE WHEN 价格区间分组 ==========

SELECT
  CASE
    WHEN price < 3000 THEN '0-3000'
    WHEN price BETWEEN 3000 AND 6000 THEN '3000-6000'
    WHEN price > 6000 THEN '6000以上'
  END AS price_range,
  COUNT(*) AS cnt,
  AVG(stock) AS avg_stock
FROM goods
GROUP BY price_range
ORDER BY price_range;


-- ========== 2. WHERE + HAVING 组合 ==========
-- WHERE 先过滤行 → GROUP BY 分组 → HAVING 过滤分组

SELECT
  CASE
    WHEN price < 3000 THEN '0-3000'
    WHEN price BETWEEN 3000 AND 6000 THEN '3000-6000'
    WHEN price > 6000 THEN '6000以上'
  END AS price_range,
  COUNT(*) AS cnt,
  AVG(stock) AS avg_stock
FROM goods
WHERE price >= 2
GROUP BY price_range
HAVING COUNT(*) >= 2
ORDER BY price_range;


-- ========== 3. 子查询：库存超过平均值的商品 ==========

SELECT id, name, price, stock
FROM goods
WHERE stock > (SELECT AVG(stock) FROM goods);
