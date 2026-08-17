-- ============================================================
-- H2 开发库种子数据：商品域测试数据
-- 对应 docs/sql/01_unimarket_common.sql 表结构
-- ============================================================

-- 分类树（三级）
INSERT INTO category (category_id, parent_id, category_name, level, sort_order, status) VALUES
  ('C001', '0',    '数码',   1, 100, 'ENABLE'),
  ('C002', '0',    '服饰',   1, 200, 'ENABLE'),
  ('C101', 'C001', '手机',   2, 110, 'ENABLE'),
  ('C102', 'C001', '耳机',   2, 120, 'ENABLE'),
  ('C201', 'C002', 'T恤',    2, 210, 'ENABLE');

-- 商品 SPU
INSERT INTO product (product_id, name, sub_title, category_id, main_image, original_price, status, sort_order) VALUES
  ('P001', '旗舰手机 Pro', '骁龙旗舰 · 一亿像素', 'C101', 'https://picsum.photos/seed/p001/400', 4999.00, 'ON_SHELF',  100),
  ('P002', '降噪蓝牙耳机', '主动降噪 · 30h续航',  'C102', 'https://picsum.photos/seed/p002/400', 899.00,  'ON_SHELF',  90),
  ('P003', '纯棉印花T恤',  '新疆长绒棉 · 透气',   'C201', 'https://picsum.photos/seed/p003/400', 129.00,  'ON_SHELF',  80),
  ('P004', '下架测试商品',  '该商品已下架',        'C101', 'https://picsum.photos/seed/p004/400', 2999.00, 'OFF_SHELF', 10);

-- SKU（P001 两规格，P002/P003 各一规格，P004 下架）
INSERT INTO sku (sku_id, product_id, sku_name, sku_attrs, price, stock, status) VALUES
  ('S001', 'P001', '旗舰手机 Pro 8+128G 黑', '{"颜色":"黑","存储":"128G"}', 4999.00, 500,  'ENABLE'),
  ('S002', 'P001', '旗舰手机 Pro 8+256G 蓝', '{"颜色":"蓝","存储":"256G"}', 5499.00, 300,  'ENABLE'),
  ('S003', 'P002', '降噪耳机 白色',          '{"颜色":"白"}',                899.00,  1000, 'ENABLE'),
  ('S004', 'P003', 'T恤 白色 L',            '{"颜色":"白","尺码":"L"}',      129.00,  2000, 'ENABLE'),
  ('S005', 'P004', '下架商品 SKU',          '{"颜色":"黑"}',                2999.00, 50,   'ENABLE');
