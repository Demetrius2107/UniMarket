package cn.unimarket.types.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应封装。
 * <p>所有接口（含异常）统一返回 {@code Response<T>}，见《接口开发规范》§5。
 * <p>禁止返回裸对象/裸 List；禁止 data 为 null 却用 200 表示失败。
 *
 * @param <T> 业务数据类型
 */
public class Response<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 业务码：200 成功，非 200 失败 */
    private int code;

    /** 人类可读提示，用于前端 toast */
    private String message;

    /** 业务数据；无数据时返回 null */
    private T data;

    /** 链路追踪 ID，排障关键 */
    private String traceId;

    public Response() {
    }

    private Response(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(200, "success", data, null);
    }

    public static <T> Response<T> success() {
        return new Response<>(200, "success", null, null);
    }

    public static <T> Response<T> failure(int code, String message) {
        return new Response<>(code, message, null, null);
    }

    public boolean isSuccess() {
        return code == 200;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
