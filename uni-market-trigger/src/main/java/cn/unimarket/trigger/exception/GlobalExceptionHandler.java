package cn.unimarket.trigger.exception;

import cn.unimarket.types.common.Response;
import cn.unimarket.types.exception.BizException;
import cn.unimarket.types.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。所有异常统一转 {@link Response}，禁止裸抛堆栈给前端（接口规范§5）。
 * <p>traceId 注入由后续 AOP 切面补齐（Phase 1 简化）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：按 ErrorCode 返回 */
    @ExceptionHandler(BizException.class)
    public Response<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getErrorCode().getCode(), e.getMessage());
        return Response.failure(e.getErrorCode().getCode(), e.getMessage());
    }

    /** 参数校验失败：@Valid @RequestBody */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 {}", msg);
        return Response.failure(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 表单参数校验失败 */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Response.failure(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 单参数校验失败：@RequestParam @Validated */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleConstraint(ConstraintViolationException e) {
        return Response.failure(ErrorCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * 方法级参数校验失败：Spring 6.2+ 对 {@code @RequestHeader @NotBlank} 等触发的新异常类型。
     * <p>Spring Boot 3.4 起部分校验改走本异常而非 {@link MethodArgumentNotValidException}。
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleMethodValidation(HandlerMethodValidationException e) {
        String msg = e.getParameterValidationResults().stream()
                .map(r -> r.getResolvableErrors().stream()
                        .map(err -> err.getDefaultMessage())
                        .reduce("", (a, b) -> a.isEmpty() ? b : a + "; " + b))
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "; " + b);
        log.warn("方法参数校验失败 {}", msg);
        return Response.failure(ErrorCode.PARAM_ERROR.getCode(), msg);
    }

    /** 请求体不可读（JSON 格式错误） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Response.failure(ErrorCode.PARAM_ERROR.getCode(), "请求体格式错误");
    }

    /** 必填请求头缺失（如 X-User-Id） */
    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Response<Void> handleMissingHeader(MissingRequestHeaderException e) {
        return Response.failure(ErrorCode.PARAM_ERROR.getCode(), "请求头缺失: " + e.getHeaderName());
    }

    /** 兜底：未捕获异常 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<Void> handleAny(Exception e) {
        log.error("系统异常", e);
        return Response.failure(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
