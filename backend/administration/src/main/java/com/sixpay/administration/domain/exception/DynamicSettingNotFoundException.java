package com.sixpay.administration.domain.exception;

public class DynamicSettingNotFoundException extends RuntimeException {
    public DynamicSettingNotFoundException(String key) {
        super("Unknown dynamic setting: " + key);
    }
}
