package com.sixpay.administration.domain.model;

public enum SettingClassification {
    DYNAMIC_OPERATIONAL(true),
    DEPLOYMENT_CONFIG(false),
    SECRET(false),
    CONTRACT_STATIC(false);

    private final boolean dynamicallyMutable;

    SettingClassification(boolean dynamicallyMutable) {
        this.dynamicallyMutable = dynamicallyMutable;
    }

    public boolean dynamicallyMutable() {
        return dynamicallyMutable;
    }
}
