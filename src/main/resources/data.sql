DELETE FROM seckill_order;
DELETE FROM seckill_goods;

INSERT INTO goods (id, name, price, stock) VALUES
    (1, 'iPhone 15', 5999.00, 100),
    (2, 'MacBook Pro', 15999.00, 50),
    (3, 'AirPods Pro', 1999.00, 200),
    (4, 'Apple Watch Ultra', 6999.00, 80),
    (5, 'iPad Air', 4999.00, 60),
    (6, 'Sony WH-1000XM5', 2499.00, 150),
    (7, 'Nintendo Switch OLED', 2599.00, 120),
    (8, 'Dyson V15 吸尘器', 4990.00, 40)
ON DUPLICATE KEY UPDATE name=VALUES(name), price=VALUES(price), stock=VALUES(stock);

INSERT INTO seckill_goods VALUES (1, 100) ON DUPLICATE KEY UPDATE stock=VALUES(stock);
INSERT INTO seckill_goods VALUES (2, 50) ON DUPLICATE KEY UPDATE stock=VALUES(stock);
