package com.sixpay.administration.api;

import com.sixpay.administration.domain.exception.DynamicSettingNotFoundException;
import com.sixpay.administration.domain.exception.DynamicSettingVersionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DynamicSettingManagementController.class)
public class DynamicSettingApiExceptionHandler {

    @ExceptionHandler({DynamicSettingNotFoundException.class, DynamicSettingVersionNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        detail.setTitle("Dynamic setting not found");
        detail.setDetail(exception.getMessage());
        detail.setProperty("code", "DYNAMIC_SETTING_NOT_FOUND");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalid(IllegalArgumentException exception) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Invalid dynamic setting request");
        detail.setDetail(exception.getMessage());
        detail.setProperty("code", "DYNAMIC_SETTING_INVALID");
        return detail;
    }
}
