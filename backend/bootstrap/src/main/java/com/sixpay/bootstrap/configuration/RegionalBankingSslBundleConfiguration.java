package com.sixpay.bootstrap.configuration;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Enumeration;
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/** Registers the existing named Spring SSL bundle only when outbound banking mTLS is enabled. */
@Configuration(proxyBeanMethods = false)
@Profile("production")
public class RegionalBankingSslBundleConfiguration {

    @Bean
    SslBundleRegistrar regionalBankingSslBundleRegistrar(
            Environment env, ResourceLoader resources) {
        return registry -> {
            if (!env.getProperty("SIXPAY_BANKING_MTLS_ENABLED", Boolean.class, false)) {
                return;
            }
            String keyLocation = required(env, "AMPLITUDE_PAYMENT_KEYSTORE_LOCATION");
            String keyPassword = required(env, "AMPLITUDE_PAYMENT_KEYSTORE_PASSWORD");
            String trustLocation = required(env, "AMPLITUDE_PAYMENT_TRUSTSTORE_LOCATION");
            String trustPassword = required(env, "AMPLITUDE_PAYMENT_TRUSTSTORE_PASSWORD");
            KeyStore keys = load(resources, keyLocation, keyPassword);
            KeyStore trust = load(resources, trustLocation, trustPassword);
            boolean privateKey = false;
            try {
                Enumeration<String> aliases = keys.aliases();
                while (aliases.hasMoreElements()) {
                    if (keys.isKeyEntry(aliases.nextElement())) privateKey = true;
                }
                if (!privateKey || trust.size() == 0) {
                    throw new IllegalStateException("Banking mTLS requires a private key and trust roots");
                }
            }
            catch (Exception ex) {
                throw new IllegalStateException("Invalid banking mTLS identity/trust material", ex);
            }
            String name = env.getProperty("AMPLITUDE_PAYMENT_SSL_BUNDLE", "amplitude-payment-client");
            registry.registerBundle(name, SslBundle.of(
                    SslStoreBundle.of(keys, keyPassword, trust),
                    SslBundleKey.of(keyPassword),
                    SslOptions.of(null, new String[]{"TLSv1.3"}),
                    "TLSv1.3"));
        };
    }

    private static String required(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required banking mTLS setting: " + key);
        }
        return value;
    }

    private static KeyStore load(ResourceLoader resources, String path, String password) {
        try (InputStream in = resources.getResource(path).getInputStream()) {
            KeyStore store = KeyStore.getInstance("PKCS12");
            store.load(in, password.toCharArray());
            return store;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Cannot load banking mTLS PKCS12 resource", ex);
        }
    }
}
