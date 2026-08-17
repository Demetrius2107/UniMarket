-- ============================================================
-- UniMarket 公共库 DDL（db00：商品/营销/物流/首页等共享域）
-- 对应 SDS §7.2 核心表清单
-- 字符集：utf8mb4 / utf8mb4_unicode_ci；引擎：InnoDB
-- Phase 1 仅建商品域三表（category/product/sku），其余域随阶段推进补齐
-- ============================================================

CREATE DATABASE IF NOT EXISTS `unimarket_common`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `unimarket_common`;

-- ------------------------------------------------------------
-- 商品分类树
-- 对应 SRS FR-PRODUCT-03 / SDS §7.2 category
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `category` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `category_id`   VARCHAR(32)     NOT NULL                COMMENT '分类业务ID（雪花）',
  `parent_id`     VARCHAR(32)     NOT NULL DEFAULT '0'    COMMENT '父分类ID，根分类为 0',
  `category_name` VARCHAR(64)     NOT NULL                COMMENT '分类名称',
  `level`         TINYINT         NOT NULL DEFAULT 1      COMMENT '层级：1一级/2二级/3三级',
  `sort_order`    INT             NOT NULL DEFAULT 0      COMMENT '同级排序，升序',
  `icon`          VARCHAR(255)             DEFAULT NULL   COMMENT '分类图标URL',
  `status`        VARCHAR(16)     NOT NULL DEFAULT 'ENABLE' COMMENT 'ENABLE启用/DISABLE禁用',
  `deleted`       TINYINT         NOT NULL DEFAULT 0      COMMENT '0未删/1已删',
  `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_id` (`category_id`),
  KEY `idx_parent_status` (`parent_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类树（三级）';

-- ------------------------------------------------------------
-- 商品 SPU
-- 对应 SRS FR-PRODUCT-01/02/05 / SDS §7.2 product
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `product` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `product_id`     VARCHAR(32)     NOT NULL                COMMENT '商品业务ID（雪花）',
  `name`           VARCHAR(128)    NOT NULL                COMMENT '商品名称',
  `sub_title`      VARCHAR(255)             DEFAULT NULL   COMMENT '副标题/卖点',
  `category_id`    VARCHAR(32)     NOT NULL                COMMENT '所属分类ID',
  `main_image`     VARCHAR(255)             DEFAULT NULL   COMMENT '主图URL',
  `images`         VARCHAR(1024)            DEFAULT NULL   COMMENT '轮播图URL列表，逗号分隔',
  `detail`         MEDIUMTEXT               DEFAULT NULL   COMMENT '富文本详情',
  `original_price` DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '原价（元，展示用）',
  `status`         VARCHAR(16)     NOT NULL DEFAULT 'ON_SHELF' COMMENT 'ON_SHELF上架/OFF_SHELF下架',
  `sort_order`     INT             NOT NULL DEFAULT 0      COMMENT '排序权重，越大越靠前',
  `deleted`        TINYINT         NOT NULL DEFAULT 0      COMMENT '0未删/1已删',
  `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`),
  KEY `idx_category_status` (`category_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品 SPU';

-- ------------------------------------------------------------
-- 商品 SKU
-- 对应 SRS FR-PRODUCT-02 / SDS §7.2 sku
-- 库存扣减走 Redis INCR + DB 行锁条件更新（SDS §10.3），stock 为 DB 基准库存
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sku` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `sku_id`     VARCHAR(32)     NOT NULL                COMMENT 'SKU业务ID（雪花）',
  `product_id` VARCHAR(32)     NOT NULL                COMMENT '所属商品ID',
  `sku_name`   VARCHAR(128)    NOT NULL                COMMENT 'SKU名称（规格组合）',
  `sku_attrs`  VARCHAR(512)             DEFAULT NULL   COMMENT '规格属性JSON，如 {"颜色":"红","尺码":"L"}',
  `price`      DECIMAL(10,2)   NOT NULL DEFAULT 0.00   COMMENT '销售单价（元）',
  `stock`      INT             NOT NULL DEFAULT 0      COMMENT 'DB基准库存（扣减走条件更新）',
  `image`      VARCHAR(255)             DEFAULT NULL   COMMENT 'SKU图片URL',
  `status`     VARCHAR(16)     NOT NULL DEFAULT 'ENABLE' COMMENT 'ENABLE启用/DISABLE禁用',
  `deleted`    TINYINT         NOT NULL DEFAULT 0      COMMENT '0未删/1已删',
  `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品 SKU';
