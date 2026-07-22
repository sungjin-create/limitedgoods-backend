package com.limitedgoods.limitedgoods.common.exception;

import com.limitedgoods.limitedgoods.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {

        ErrorCode errorCode = e.getErrorCode();

        log.warn(
                "event=business_exception component=application " +
                        "method={} path={} errorCode={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                errorCode.getCode(),
                e.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(
                        ApiResponse.fail(
                                errorCode.getCode(),
                                e.getMessage()
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception e,
            HttpServletRequest request) {

        log.error(
                "event=unexpected_exception component=application " +
                        "method={} path={}",
                request.getMethod(),
                request.getRequestURI(),
                e
        );

        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail(
                        "INTERNAL_SERVER_ERROR",
                        "서버 오류가 발생했습니다."
                ));
    }
}