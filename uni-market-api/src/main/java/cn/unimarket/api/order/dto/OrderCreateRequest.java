package cn.unimarket.api.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 创建订单请求。对应接口 {@code POST /api/v1/order/create}（FR-ORDER-02）。
 * <p>Phase 1 最小闭环：普通下单（NORMAL），暂不含优惠券/积分抵扣，后续 Phase 6/5 叠加。
 * <p>幂等：前端对同一笔下单请求生成相同 {@link #bizId}，服务端按 {@code userId+bizId} 去重（BR-08/接口规范§9）。
 */
public class OrderCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 业务防重 ID。前端对同一次下单提交生成稳定 bizId（如 UUID），
     * 服务端按 {@code userId+bizId} 唯一约束兜底，重复提交返回原订单。
     */
    @NotBlank(message = "bizId不能为空")
    @Size(max = 64, message = "bizId长度不超过64")
    private String bizId;

    /** 收货地址 ID */
    @NotBlank(message = "收货地址ID不能为空")
    private String addressId;

    /** 订单明细列表，至少一项 */
    @NotEmpty(message = "订单明细不能为空")
    @Size(max = 50, message = "单次下单明细不超过50项")
    private List<OrderItemDTO> items;

    /** 买家备注，可空 */
    @Size(max = 200, message = "备注长度不超过200")
    private String remark;

    public OrderCreateRequest() {
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
