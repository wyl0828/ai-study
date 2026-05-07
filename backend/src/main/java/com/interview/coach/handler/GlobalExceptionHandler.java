package com.interview.coach.handler;

import com.interview.coach.vo.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ApiResponse<Void> handleValidationException(Exception ex) {
        return ApiResponse.fail(400, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.fail(500, "internal server error: " + ex.getMessage());
    }
}
