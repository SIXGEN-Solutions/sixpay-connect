package com.sixpay.partner;

/**
 * Marker type used to locate the Partner module without relying on package strings.
 */
public final class PartnerModule {

    private PartnerModule() {
        throw new IllegalStateException("Marker class");
    }
}
