-- ============================================================
-- H2 开发库建表脚本（MySQL 兼容模式）
-- 对应 SDS §7.2 order / order_item 表结构
-- 生产 MySQL 建表脚本见 docs/sql/
-- ============================================================

CREATE TABLE IF NOT EXISTS `order` (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  order_id        VARCHAR(32)  NOT NULL COMMENT '订单ID（雪花）',
  user_id         VARCHAR(32)  NOT NULL COMMENT '用户ID（分片键）',
  biz_id          VARCHAR(64)  NOT NULL COMMENT '业务防重ID（userId+bizId幂等）',
  order_type      VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/GROUP_BUY/CREDIT',
  activity_id     VARCHAR(32)           COMMENT '关联营销活动ID',
  team_id         VARCHAR(32)           COMMENT '关联拼团队伍ID',
  total_amount    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '订单总金额',
  discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
  pay_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '实付金额',
  status          VARCHAR(16)  NOT NULL DEFAULT 'CREATE' COMMENT 'CREATE/PAY_WAIT/PAY_SUCCESS/DEAL_DONE/CLOSE/REFUND',
  pay_channel     VARCHAR(16)           COMMENT 'ALIPAY/WXPAY',
  out_trade_no    VARCHAR(64)           COMMENT '第三方支付单号',
  pay_time        DATETIME              COMMENT '支付时间',
  address_id      VARCHAR(32)  NOT NULL COMMENT '收货地址ID',
  remark          VARCHAR(200)          COMMENT '买家备注',
  deleted         TINYINT      NOT NULL DEFAULT 0 COMMENT '0未删/1已删',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_id (order_id),
  UNIQUE KEY uk_user_biz (user_id, biz_id),
  KEY idx_user_status (user_id, status)
);

CREATE TABLE IF NOT EXISTS order_item (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  order_id        VARCHAR(32)  NOT NULL COMMENT '订单ID',
  product_id      VARCHAR(32)  NOT NULL COMMENT '商品ID',
  sku_id          VARCHAR(32)  NOT NULL COMMENT 'SKU ID',
  quantity        INT          NOT NULL DEFAULT 1 COMMENT '购买数量',
  original_price  DECIMAL(10,2) NOT NULL COMMENT '原始单价快照',
  actual_price    DECIMAL(10,2) NOT NULL COMMENT '实际单价',
  create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_order_id (order_id)
);

-- ============================================================
-- 商品域（对应 docs/sql/01_unimarket_common.sql）
-- ============================================================

CREATE TABLE IF NOT EXISTS category (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  category_id   VARCHAR(32)  NOT NULL                COMMENT '分类业务ID',
  parent_id     VARCHAR(32)  NOT NULL DEFAULT '0'    COMMENT '父分类ID，根分类为0',
  category_name VARCHAR(64)  NOT NULL                COMMENT '分类名称',
  level         TINYINT      NOT NULL DEFAULT 1      COMMENT '层级：1一级/2二级/3三级',
  sort_order    INT          NOT NULL DEFAULT 0      COMMENT '同级排序',
  icon          VARCHAR(255)          DEFAULT NULL   COMMENT '分类图标URL',
  status        VARCHAR(16)  NOT NULL DEFAULT 'ENABLE' COMMENT 'ENABLE/DISABLE',
  deleted       TINYINT      NOT NULL DEFAULT 0      COMMENT '0未删/1已删',
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_category_id (category_id),
  KEY idx_parent_status (parent_id, status)
);

CREATE TABLE IF NOT EXISTS product (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  product_id     VARCHAR(32)  NOT NULL                COMMENT '商品业务ID',
  name           VARCHAR(128) NOT NULL                COMMENT '商品名称',
  sub_title      VARCHAR(255)          DEFAULT NULL   COMMENT '副标题',
  category_id    VARCHAR(32)  NOT NULL                COMMENT '分类ID',
  main_image     VARCHAR(255)          DEFAULT NULL   COMMENT '主图URL',
  images         VARCHAR(1024)         DEFAULT NULL   COMMENT '轮播图URL列表',
  detail         MEDIUMTEXT            DEFAULT NULL   COMMENT '富文本详情',
  original_price DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '原价',
  status         VARCHAR(16)  NOT NULL DEFAULT 'ON_SHELF' COMMENT 'ON_SHELF/OFF_SHELF',
  sort_order     INT          NOT NULL DEFAULT 0      COMMENT '排序权重',
  deleted        TINYINT      NOT NULL DEFAULT 0      COMMENT '0未删/1已删',
  create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_id (product_id),
  KEY idx_category_status (category_id, status)
);

CREATE TABLE IF NOT EXISTS sku (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  sku_id      VARCHAR(32)  NOT NULL                COMMENT 'SKU业务ID',
  product_id  VARCHAR(32)  NOT NULL                COMMENT '商品ID',
  sku_name    VARCHAR(128) NOT NULL                COMMENT 'SKU名称',
  sku_attrs   VARCHAR(512)          DEFAULT NULL   COMMENT '规格属性JSON',
  price       DECIMAL(10,2) NOT NULL DEFAULT 0.00  COMMENT '销售单价',
  stock       INT          NOT NULL DEFAULT 0      COMMENT 'DB基准库存',
  image       VARCHAR(255)          DEFAULT NULL   COMMENT 'SKU图片URL',
  status      VARCHAR(16)  NOT NULL DEFAULT 'ENABLE' COMMENT 'ENABLE/DISABLE',
  deleted     TINYINT      NOT NULL DEFAULT 0      COMMENT '0未删/1已删',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_sku_id (sku_id),
  KEY idx_product_id (product_id)
);
