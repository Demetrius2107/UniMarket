package cn.unimarket.api.order.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订单列表查询请求。对应 {@code GET /api/v1/order/list}（FR-ORDER-04）。
 * <p>分页参数 page 从 1 开始，size 默认 10、上限 100（见接口规范§7）。
 */
public class OrderListRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单状态筛选，可空（查全部）：CREATE/PAY_WAIT/PAY_SUCCESS/DEAL_DONE/CLOSE/REFUND */
    private String status;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页大小，默认 10，最大 100 */
    private Integer size = 10;

    public OrderListRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
