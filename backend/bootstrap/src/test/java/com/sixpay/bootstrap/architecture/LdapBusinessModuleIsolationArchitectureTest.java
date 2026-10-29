package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LdapBusinessModuleIsolationArchitectureTest {
    private static final Path ROOT=Path.of("..");
    private static final List<String> MODULES=List.of(
            "partner","customer","payment","accounting",
            "reporting","notification","administration");
    private static final List<String> FORBIDDEN=List.of(
            "org.springframework.ldap","ldaptemplate","ldapcontextsource",
            "activedirectoryldap","sixpay.security.authentication.ldap",
            "objectguid","samaccountname");

    @Test
    void businessModulesNeverDependOnLdapProviderInternals() throws Exception {
        List<String> violations=new ArrayList<>();
        for(String module:MODULES){
            Path root=ROOT.resolve(module+"/src/main/java");
            if(!Files.isDirectory(root)) continue;
            try(var files=Files.walk(root)){
                for(Path file:files.filter(Files::isRegularFile)
                        .filter(x->x.toString().endsWith(".java")).toList()){
                    String source=Files.readString(file).toLowerCase(Locale.ROOT);
                    for(String marker:FORBIDDEN)
                        if(source.contains(marker))
                            violations.add(module+": "+file+" -> "+marker);
                }
            }
        }
        assertTrue(violations.isEmpty(),
                ()->"LDAP provider internals leaked into business modules: "+violations);
    }
}
