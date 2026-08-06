package com.sixpay.integration.http;
import java.util.UUID;
public final class UuidRequestIdGenerator implements RequestIdGenerator {
    @Override public String generate() { return UUID.randomUUID().toString(); }
}
