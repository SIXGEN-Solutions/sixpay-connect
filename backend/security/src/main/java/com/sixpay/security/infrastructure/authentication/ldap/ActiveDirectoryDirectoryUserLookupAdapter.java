package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.model.DirectoryAccountStatus;
import com.sixpay.security.application.model.DirectoryUserProfile;
import com.sixpay.security.application.port.output.DirectoryUserLookupPort;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class ActiveDirectoryDirectoryUserLookupAdapter
        implements DirectoryUserLookupPort {

    private static final int UF_ACCOUNTDISABLE = 0x0002;
    private static final int UF_LOCKOUT = 0x0010;
    private static final int UF_PASSWORD_EXPIRED = 0x800000;
    private static final long WINDOWS_EPOCH_OFFSET_SECONDS = 11_644_473_600L;
    private static final long WINDOWS_TICKS_PER_SECOND = 10_000_000L;

    private final AuthenticationCapabilitiesProperties.Ldap properties;
    private final LongSupplier nanoTime;
    private final Supplier<Instant> now;

    public ActiveDirectoryDirectoryUserLookupAdapter(
            AuthenticationCapabilitiesProperties.Ldap properties
    ) {
        this(properties, System::nanoTime, Instant::now);
    }

    ActiveDirectoryDirectoryUserLookupAdapter(
            AuthenticationCapabilitiesProperties.Ldap properties,
            LongSupplier nanoTime,
            Supplier<Instant> now
    ) {
        this.properties = Objects.requireNonNull(properties, "LDAP properties must not be null");
        this.nanoTime = Objects.requireNonNull(nanoTime, "LDAP monotonic clock must not be null");
        this.now = Objects.requireNonNull(now, "LDAP wall clock must not be null");
    }

    @Override
    public DirectoryUserProfile lookupByUsername(String username) {
        String normalizedUsername = normalizeUsername(username);
        long deadlineNanos = deadlineNanos(properties.authenticationTimeout().toNanos());

        try {
            LdapTemplate serviceTemplate = new LdapTemplate(
                    contextSource(
                            properties.serviceAccountDn(),
                            properties.serviceAccountPassword(),
                            deadlineNanos
                    )
            );

            List<DirectoryUser> users = serviceTemplate.search(
                    properties.userSearchBase(),
                    resolveSearchFilter(normalizedUsername),
                    SearchControls.SUBTREE_SCOPE,
                    attributesMapper()
            );

            requireWithinBudget(deadlineNanos);

            if (users.size() != 1) {
                throw new LdapAuthenticationFailedException();
            }

            DirectoryUser user = users.getFirst();

            return new DirectoryUserProfile(
                    user.username(),
                    user.displayName(),
                    user.email(),
                    properties.trustDomain(),
                    user.stableSubject(),
                    accountStatus(user.accountState(), now.get())
            );
        } catch (LdapAuthenticationFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LdapAuthenticationFailedException(exception);
        }
    }

    LdapContextSource contextSource(
            String principal,
            String credentials,
            long deadlineNanos
    ) {
        long remainingMillis = remainingMillis(deadlineNanos);
        long perPhaseBudgetMillis = Math.max(1L, remainingMillis / 2L);

        LdapContextSource source = new LdapContextSource();
        source.setUrls(properties.urls().toArray(String[]::new));
        source.setBase(properties.baseDn());
        source.setUserDn(principal);
        source.setPassword(credentials);

        java.util.Hashtable<String, Object> environment = new java.util.Hashtable<>();
        if ("objectGUID".equalsIgnoreCase(properties.subjectAttribute())) {
            environment.put(
                    "java.naming.ldap.attributes.binary",
                    properties.subjectAttribute()
            );
        }

        environment.put(
                "com.sun.jndi.ldap.connect.timeout",
                String.valueOf(
                        Math.min(
                                properties.connectTimeout().toMillis(),
                                perPhaseBudgetMillis
                        )
                )
        );
        environment.put(
                "com.sun.jndi.ldap.read.timeout",
                String.valueOf(
                        Math.min(
                                properties.readTimeout().toMillis(),
                                perPhaseBudgetMillis
                        )
                )
        );
        source.setBaseEnvironmentProperties(environment);
        source.afterPropertiesSet();
        return source;
    }

    long deadlineNanos(long timeoutNanos) {
        long startedAt = nanoTime.getAsLong();
        try {
            return Math.addExact(startedAt, timeoutNanos);
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    void requireWithinBudget(long deadlineNanos) {
        if (deadlineNanos - nanoTime.getAsLong() <= 0L) {
            throw new LdapAuthenticationFailedException();
        }
    }

    private long remainingMillis(long deadlineNanos) {
        long remainingNanos = deadlineNanos - nanoTime.getAsLong();
        if (remainingNanos <= 0L) {
            throw new LdapAuthenticationFailedException();
        }
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
    }

    String resolveSearchFilter(String username) {
        String configured = properties.userSearchFilter();
        String escapedValue =
                new EqualsFilter(properties.loginAttribute(), username)
                        .encode()
                        .replace("(" + properties.loginAttribute() + "=", "")
                        .replaceFirst("\\)$", "");

        return configured.replace("{0}", escapedValue);
    }

    private AttributesMapper<DirectoryUser> attributesMapper() {
        return attributes -> new DirectoryUser(
                requiredString(attributes, "distinguishedName"),
                requiredString(attributes, properties.usernameAttribute()),
                optionalString(attributes, "displayName"),
                optionalString(attributes, "mail"),
                stableSubject(attributes, properties.subjectAttribute()),
                accountState(attributes)
        );
    }

    static DirectoryAccountState accountState(Attributes attributes)
            throws NamingException {
        int userAccountControl = optionalInt(attributes, "userAccountControl", 0);
        int computedControl = optionalInt(
                attributes,
                "msDS-User-Account-Control-Computed",
                0
        );
        long accountExpires = optionalLong(attributes, "accountExpires", 0L);
        long pwdLastSet = optionalLong(attributes, "pwdLastSet", -1L);

        return new DirectoryAccountState(
                (userAccountControl & UF_ACCOUNTDISABLE) != 0,
                (computedControl & UF_LOCKOUT) != 0,
                (computedControl & UF_PASSWORD_EXPIRED) != 0,
                accountExpires,
                pwdLastSet
        );
    }

    static DirectoryAccountStatus accountStatus(
            DirectoryAccountState state,
            Instant instant
    ) {
        if (state.disabled()) {
            return DirectoryAccountStatus.DISABLED;
        }
        if (state.locked()) {
            return DirectoryAccountStatus.LOCKED;
        }
        if (state.passwordExpired()) {
            return DirectoryAccountStatus.PASSWORD_EXPIRED;
        }
        if (state.passwordMustChange()) {
            return DirectoryAccountStatus.PASSWORD_CHANGE_REQUIRED;
        }
        if (state.accountExpired(instant)) {
            return DirectoryAccountStatus.EXPIRED;
        }
        return DirectoryAccountStatus.ACTIVE;
    }

    private static String requiredString(
            Attributes attributes,
            String name
    ) throws NamingException {
        Attribute attribute = attributes.get(name);
        if (attribute == null || attribute.get() == null) {
            throw new LdapAuthenticationFailedException();
        }
        String value = attribute.get().toString();
        if (value.isBlank()) {
            throw new LdapAuthenticationFailedException();
        }
        return value;
    }

    private static String optionalString(
            Attributes attributes,
            String name
    ) throws NamingException {
        Attribute attribute = attributes.get(name);
        if (attribute == null || attribute.get() == null) {
            return null;
        }
        String value = attribute.get().toString();
        return value.isBlank() ? null : value;
    }

    private static int optionalInt(
            Attributes attributes,
            String name,
            int defaultValue
    ) throws NamingException {
        Attribute attribute = attributes.get(name);
        if (attribute == null || attribute.get() == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(attribute.get().toString());
        } catch (NumberFormatException exception) {
            throw new LdapAuthenticationFailedException(exception);
        }
    }

    private static long optionalLong(
            Attributes attributes,
            String name,
            long defaultValue
    ) throws NamingException {
        Attribute attribute = attributes.get(name);
        if (attribute == null || attribute.get() == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(attribute.get().toString());
        } catch (NumberFormatException exception) {
            throw new LdapAuthenticationFailedException(exception);
        }
    }

    private static String stableSubject(
            Attributes attributes,
            String attributeName
    ) throws NamingException {
        Attribute attribute = attributes.get(attributeName);
        if (attribute == null || attribute.get() == null) {
            throw new LdapAuthenticationFailedException();
        }

        Object raw = attribute.get();

        if ("objectGUID".equalsIgnoreCase(attributeName)
                && raw instanceof byte[] bytes) {
            return objectGuid(bytes);
        }

        String value = raw.toString();
        if (value.isBlank()) {
            throw new LdapAuthenticationFailedException();
        }
        return value;
    }

    static String objectGuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new LdapAuthenticationFailedException();
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int data1 = Integer.reverseBytes(buffer.getInt());
        short data2 = Short.reverseBytes(buffer.getShort());
        short data3 = Short.reverseBytes(buffer.getShort());

        long msb =
                ((long) data1 & 0xffffffffL) << 32
                        | ((long) data2 & 0xffffL) << 16
                        | ((long) data3 & 0xffffL);

        long lsb = buffer.getLong();

        return new UUID(msb, lsb).toString();
    }

    private static String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new LdapAuthenticationFailedException();
        }
        return username.trim();
    }

    record DirectoryAccountState(
            boolean disabled,
            boolean locked,
            boolean passwordExpired,
            long accountExpires,
            long pwdLastSet
    ) {
        boolean passwordMustChange() {
            return pwdLastSet == 0L;
        }

        boolean accountExpired(Instant instant) {
            if (accountExpires == 0L
                    || accountExpires == Long.MAX_VALUE) {
                return false;
            }

            long epochSecond =
                    accountExpires / WINDOWS_TICKS_PER_SECOND
                            - WINDOWS_EPOCH_OFFSET_SECONDS;
            long nanoAdjustment =
                    (accountExpires % WINDOWS_TICKS_PER_SECOND) * 100L;

            Instant expiresAt = Instant.ofEpochSecond(
                    epochSecond,
                    nanoAdjustment
            );

            return !expiresAt.isAfter(instant);
        }
    }

    record DirectoryUser(
            String distinguishedName,
            String username,
            String displayName,
            String email,
            String stableSubject,
            DirectoryAccountState accountState
    ) {
    }
}
