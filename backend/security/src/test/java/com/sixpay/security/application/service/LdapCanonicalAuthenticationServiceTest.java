package com.sixpay.security.application.service;

import com.sixpay.security.application.exception.ExternalIdentityNotLinkedException;
import com.sixpay.security.application.exception.SixpayUserDisabledException;
import com.sixpay.security.application.port.input.LdapAuthenticationCommand;
import com.sixpay.security.authentication.AuthenticatedUser;
import com.sixpay.security.domain.authentication.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class LdapCanonicalAuthenticationServiceTest {
    private static final UUID USER=UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

    @Test void linkedActiveIdentityResolvesSixpayAuthorities() {
        var resolver=resolver(Map.of(key("regionale-ldap","s1"),link(account(USER,"user",SixpayUserAccountStatus.ACTIVE),AuthenticationIdentityType.LDAP,"regionale-ldap","s1")));
        AuthenticatedUser user=service("regionale-ldap","s1",resolver).authenticate(new LdapAuthenticationCommand("user","pwd"));
        assertThat(user.subject()).isEqualTo(USER.toString());
        assertThat(user.roles()).containsExactly("ADMIN");
        assertThat(user.permissions()).containsExactly("SCOPE_payment.read");
    }

    @Test void absentIdentityIsRefused() {
        assertThatThrownBy(() -> service("regionale-ldap","missing",resolver(Map.of()))
                .authenticate(new LdapAuthenticationCommand("user","pwd")))
                .isInstanceOf(ExternalIdentityNotLinkedException.class);
    }

    @Test void disabledSixpayAccountIsRefused() {
        var a=account(USER,"user",SixpayUserAccountStatus.DISABLED);
        var r=resolver(Map.of(key("regionale-ldap","s1"),link(a,AuthenticationIdentityType.LDAP,"regionale-ldap","s1")));
        assertThatThrownBy(() -> service("regionale-ldap","s1",r)
                .authenticate(new LdapAuthenticationCommand("user","pwd")))
                .isInstanceOf(SixpayUserDisabledException.class);
    }

    @Test void sameSubjectUnderTwoTrustDomainsIsDistinct() {
        UUID other=UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
        var r=resolver(Map.of(
                key("ldap-a","same"),link(account(USER,"a",SixpayUserAccountStatus.ACTIVE),AuthenticationIdentityType.LDAP,"ldap-a","same"),
                key("ldap-b","same"),link(account(other,"b",SixpayUserAccountStatus.ACTIVE),AuthenticationIdentityType.LDAP,"ldap-b","same")));
        var a=service("ldap-a","same",r).authenticate(new LdapAuthenticationCommand("a","pwd"));
        var b=service("ldap-b","same",r).authenticate(new LdapAuthenticationCommand("b","pwd"));
        assertThat(a.subject()).isNotEqualTo(b.subject());
    }

    @Test void localOidcAndLdapConvergeOnSameAccount() {
        var a=account(USER,"user",SixpayUserAccountStatus.ACTIVE);
        var r=resolver(Map.of(
                key("oidc","o1"),link(a,AuthenticationIdentityType.OIDC,"oidc","o1"),
                key("ldap","l1"),link(a,AuthenticationIdentityType.LDAP,"ldap","l1")));
        var local=new AuthenticatedUser(a.canonicalSubject(),a.username(),a.authorities());
        var oidc=r.resolve(AuthenticationIdentityType.OIDC,new ExternalIdentity("oidc","o1","oidc-user"));
        var ldap=r.resolve(AuthenticationIdentityType.LDAP,new ExternalIdentity("ldap","l1","ldap-user"));
        assertThat(oidc).isEqualTo(local);
        assertThat(ldap).isEqualTo(local);
    }

    private static LdapCanonicalAuthenticationService service(String provider,String subject,LinkedExternalIdentityResolver r) {
        return new LdapCanonicalAuthenticationService(
                c -> new LdapAuthenticationResult(AuthenticationIdentityType.LDAP,new ExternalIdentity(provider,subject,c.username())), r);
    }

    private static LinkedExternalIdentityResolver resolver(Map<String,LinkedUserIdentity> links) {
        return new LinkedExternalIdentityResolver((type,provider,subject) ->
                Optional.ofNullable(links.get(key(provider,subject)))
                        .filter(x -> x.identity().identityType()==type));
    }

    private static String key(String p,String s){ return p+"|"+s; }

    private static SixpayUserAccount account(UUID id,String username,SixpayUserAccountStatus status) {
        return new SixpayUserAccount(id,username,username+"@sixpay.test",status,Set.of("ADMIN"),Set.of("SCOPE_payment.read"));
    }

    private static LinkedUserIdentity link(SixpayUserAccount a,AuthenticationIdentityType type,String provider,String subject) {
        Instant now=Instant.parse("2026-09-26T12:00:00Z");
        return new LinkedUserIdentity(a,new UserIdentity(UUID.randomUUID(),a.id(),type,provider,subject,now,now));
    }
}
