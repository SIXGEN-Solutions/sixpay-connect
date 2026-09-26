package com.sixpay.security.infrastructure.authentication.ldap;

import com.sixpay.security.application.exception.LdapAuthenticationFailedException;
import com.sixpay.security.application.port.input.AuthenticateLdapIdentityUseCase;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.domain.authentication.AuthenticationIdentityType;
import com.sixpay.security.domain.authentication.ExternalIdentity;
import com.sixpay.security.domain.authentication.LdapAuthenticationResult;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ActiveDirectoryLdapAuthenticationAdapter
        implements AuthenticateLdapIdentityUseCase {

    private final AuthenticationCapabilitiesProperties.Ldap properties;

    public ActiveDirectoryLdapAuthenticationAdapter(
            AuthenticationCapabilitiesProperties.Ldap properties
    ) {
        this.properties = Objects.requireNonNull(
                properties,
                "LDAP properties must not be null"
        );
    }

    @Override
    public LdapAuthenticationResult authenticate(
            LdapAuthenticationCommand command
    ) {
        Objects.requireNonNull(
                command,
                "LDAP authentication command must not be null"
        );

        try {
            LdapTemplate serviceTemplate =
                    new LdapTemplate(
                            contextSource(
                                    properties.serviceAccountDn(),
                                    properties.serviceAccountPassword()
                            )
                    );

            String searchFilter =
                    resolveSearchFilter(
                            command.username()
                    );

            List<DirectoryUser> users =
                    serviceTemplate.search(
                            properties.userSearchBase(),
                            searchFilter,
                            SearchControls.SUBTREE_SCOPE,
                            attributesMapper()
                    );

            if (users.size() != 1) {
                throw new LdapAuthenticationFailedException();
            }

            DirectoryUser user = users.getFirst();

            // Transient user bind proves the submitted credentials.
            contextSource(
                    user.distinguishedName(),
                    command.password()
            ).getReadOnlyContext().close();

            ExternalIdentity externalIdentity =
                    new ExternalIdentity(
                            properties.trustDomain(),
                            user.stableSubject(),
                            user.username()
                    );

            return new LdapAuthenticationResult(
                    AuthenticationIdentityType.LDAP,
                    externalIdentity
            );
        } catch (LdapAuthenticationFailedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LdapAuthenticationFailedException(
                    exception
            );
        }
    }

    private LdapContextSource contextSource(
            String principal,
            String credentials
    ) {
        LdapContextSource source = new LdapContextSource();
        source.setUrls(
                properties.urls().toArray(String[]::new)
        );
        source.setBase(properties.baseDn());
        source.setUserDn(principal);
        source.setPassword(credentials);

        java.util.Hashtable<String, Object> environment =
                new java.util.Hashtable<>();
        environment.put(
                "com.sun.jndi.ldap.connect.timeout",
                String.valueOf(properties.connectTimeout().toMillis())
        );
        environment.put(
                "com.sun.jndi.ldap.read.timeout",
                String.valueOf(properties.readTimeout().toMillis())
        );
        source.setBaseEnvironmentProperties(environment);
        source.afterPropertiesSet();
        return source;
    }

    private String resolveSearchFilter(
            String username
    ) {
        String configured = properties.userSearchFilter();
        String escapedValue =
                new EqualsFilter(
                        properties.loginAttribute(),
                        username
                )
                        .encode()
                        .replace(
                                "(" + properties.loginAttribute() + "=",
                                ""
                        )
                        .replaceFirst("\\)$", "");

        return configured.replace(
                "{0}",
                escapedValue
        );
    }

    private AttributesMapper<DirectoryUser> attributesMapper() {
        return attributes -> new DirectoryUser(
                requiredString(
                        attributes,
                        "distinguishedName"
                ),
                requiredString(
                        attributes,
                        properties.usernameAttribute()
                ),
                stableSubject(
                        attributes,
                        properties.subjectAttribute()
                )
        );
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

    static String objectGuid(
            byte[] bytes
    ) {
        if (bytes == null || bytes.length != 16) {
            throw new LdapAuthenticationFailedException();
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        int data1 =
                Integer.reverseBytes(buffer.getInt());
        short data2 =
                Short.reverseBytes(buffer.getShort());
        short data3 =
                Short.reverseBytes(buffer.getShort());

        long msb =
                ((long) data1 & 0xffffffffL) << 32
                        | ((long) data2 & 0xffffL) << 16
                        | ((long) data3 & 0xffffL);

        long lsb = buffer.getLong();

        return new UUID(msb, lsb).toString();
    }

    private record DirectoryUser(
            String distinguishedName,
            String username,
            String stableSubject
    ) {
    }
}
