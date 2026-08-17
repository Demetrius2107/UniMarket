package cn.unimarket.types.exception;

import java.io.Serial;

/**
 * 业务异常。领域层/应用层校验失败时抛出，由全局异常处理器转换为 {@link ErrorCode} 响应。
 * <p>技术异常（DB 断连、空指针等）不包装成本类，直接走 500 兜底。
 */
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
