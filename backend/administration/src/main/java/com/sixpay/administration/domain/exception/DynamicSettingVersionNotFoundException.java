package com.sixpay.administration.domain.exception;

public class DynamicSettingVersionNotFoundException extends RuntimeException {
    public DynamicSettingVersionNotFoundException(String key, long version) {
        super("Dynamic setting version not found: " + key + "@" + version);
    }
}
