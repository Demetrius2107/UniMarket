package cn.unimarket.types.enums;

/**
 * 商品上下架状态。见 SRS FR-PRODUCT-05 / SDS §7.2 product.status。
 * <p>下架商品不可下单、不出现在列表；订单中已存在的下架商品快照仍有效。
 */
public enum ProductStatus {

    /** 上架（可售） */
    ON_SHELF,
    /** 下架（不可售） */
    OFF_SHELF
}
