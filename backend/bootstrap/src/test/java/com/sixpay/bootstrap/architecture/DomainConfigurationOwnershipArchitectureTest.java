package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainConfigurationOwnershipArchitectureTest {

    private static final Path BACKEND_ROOT =
            Path.of("..");

    private static final List<String> BUSINESS_MODULES =
            List.of(
                    "partner",
                    "customer",
                    "payment",
                    "accounting",
                    "reporting",
                    "notification",
                    "security",
                    "administration"
            );

    private static final Map<String, String>
            DOMAIN_PREFIXES =
            new LinkedHashMap<>();

    static {
        for (String module : BUSINESS_MODULES) {
            DOMAIN_PREFIXES.put(
                    module,
                    "sixpay." + module + "."
            );
        }
    }

    private static final Pattern CONFIG_PROPERTIES =
            Pattern.compile(
                    "@ConfigurationProperties\\s*\\([^)]*"
                            + "prefix\\s*=\\s*\"([^\"]+)\"",
                    Pattern.DOTALL
            );

    private static final Pattern VALUE =
            Pattern.compile(
                    "@Value\\s*\\(\\s*\"\\$\\{([^}:]+)"
            );

    private static final Pattern CONDITIONAL =
            Pattern.compile(
                    "@ConditionalOnProperty\\s*\\((.*?)\\)",
                    Pattern.DOTALL
            );

    @Test
    void businessModulesDoNotConsumeOtherDomainConfiguration()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (String module : BUSINESS_MODULES) {

            Path javaRoot =
                    BACKEND_ROOT.resolve(
                            module + "/src/main/java"
                    );

            if (!Files.isDirectory(javaRoot)) {
                continue;
            }

            try (var files = Files.walk(javaRoot)) {
                for (Path javaFile :
                        files.filter(Files::isRegularFile)
                                .filter(path ->
                                        path.toString()
                                                .endsWith(".java")
                                )
                                .toList()) {

                    String source =
                            Files.readString(javaFile);

                    for (String key :
                            propertyKeys(source)) {

                        String owner =
                                ownerOf(key);

                        if (owner != null
                                && !owner.equals(module)) {

                            violations.add(
                                    module
                                            + " consumes "
                                            + key
                                            + " owned by "
                                            + owner
                                            + " in "
                                            + javaFile
                            );
                        }
                    }
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Cross-domain configuration ownership "
                        + "violations: "
                        + violations
        );
    }

    private List<String> propertyKeys(
            String source
    ) {

        List<String> keys =
                new ArrayList<>();

        Matcher properties =
                CONFIG_PROPERTIES.matcher(source);

        while (properties.find()) {
            keys.add(properties.group(1));
        }

        Matcher values =
                VALUE.matcher(source);

        while (values.find()) {
            keys.add(values.group(1));
        }

        Matcher conditionals =
                CONDITIONAL.matcher(source);

        while (conditionals.find()) {

            String block =
                    conditionals.group(1);

            Matcher prefix =
                    Pattern.compile(
                            "prefix\\s*=\\s*\"([^\"]+)\""
                    ).matcher(block);

            if (!prefix.find()) {
                continue;
            }

            String key =
                    prefix.group(1);

            Matcher name =
                    Pattern.compile(
                            "name\\s*=\\s*\"([^\"]+)\""
                    ).matcher(block);

            if (name.find()) {
                key += "." + name.group(1);
            }

            keys.add(key);
        }

        return keys;
    }

    private String ownerOf(
            String key
    ) {

        String normalized =
                key.endsWith(".")
                        ? key
                        : key + ".";

        for (var entry :
                DOMAIN_PREFIXES.entrySet()) {

            if (normalized.startsWith(
                    entry.getValue()
            )) {
                return entry.getKey();
            }
        }

        return null;
    }
}
