package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BusinessModuleBoundaryArchitectureTest {

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

    private static final Pattern IMPORT_PATTERN =
            Pattern.compile(
                    "^\\s*import\\s+(?:static\\s+)?"
                            + "(com\\.sixpay\\.[A-Za-z0-9_.$*]+)"
                            + "\\s*;",
                    Pattern.MULTILINE
            );

    private static final Set<ModuleEdge> APPROVED_EDGES =
            Set.of(
                    new ModuleEdge("partner", "security"),
                    new ModuleEdge("customer", "security"),
                    new ModuleEdge("payment", "security"),
                    new ModuleEdge("administration", "security")
            );

    private static final Set<String>
            EXACT_PUBLIC_SECURITY_CONTRACTS =
            Set.of(
                    "com.sixpay.security.authentication.CurrentUserProvider",
                    "com.sixpay.security.authentication.AuthenticatedUser",
                    "com.sixpay.security.authentication.SixpayPrincipal"
            );

    private static final List<String>
            PUBLIC_SECURITY_PREFIXES =
            List.of(
                    "com.sixpay.security.authorization.",
                    "com.sixpay.security.application.port.in.",
                    "com.sixpay.security.application.port.input.",
                    "com.sixpay.security.application.model."
            );

    @Test
    void businessModulesImportOnlyReviewedPublicSurfaces()
            throws Exception {

        ScanResult scan = scanProductionJava();

        assertTrue(
                scan.violations().isEmpty(),
                () -> "Business module boundary violations: "
                        + scan.violations()
        );

        assertTrue(
                APPROVED_EDGES.containsAll(scan.edges()),
                () -> "Unreviewed business-to-business edges: "
                        + difference(scan.edges(), APPROVED_EDGES)
        );
    }

    @Test
    void productionJavaBusinessGraphIsAcyclic()
            throws Exception {

        List<String> cycles =
                findCycles(
                        adjacency(
                                scanProductionJava().edges()
                        )
                );

        assertTrue(
                cycles.isEmpty(),
                () -> "Circular Java business dependencies: "
                        + cycles
        );
    }

    @Test
    void mavenBusinessDependenciesMatchReviewedJavaEdges()
            throws Exception {

        Set<ModuleEdge> javaEdges =
                scanProductionJava().edges();

        Set<ModuleEdge> mavenEdges =
                scanMavenBusinessDependencies();

        assertTrue(
                difference(mavenEdges, javaEdges).isEmpty(),
                () -> "Unused Maven business dependencies: "
                        + difference(mavenEdges, javaEdges)
        );

        assertTrue(
                difference(javaEdges, mavenEdges).isEmpty(),
                () -> "Production Java business edges without "
                        + "matching Maven dependency: "
                        + difference(javaEdges, mavenEdges)
        );

        assertTrue(
                APPROVED_EDGES.containsAll(mavenEdges),
                () -> "Unreviewed Maven business edges: "
                        + difference(mavenEdges, APPROVED_EDGES)
        );
    }

    @Test
    void mavenBusinessDependencyGraphIsAcyclic()
            throws Exception {

        List<String> cycles =
                findCycles(
                        adjacency(
                                scanMavenBusinessDependencies()
                        )
                );

        assertTrue(
                cycles.isEmpty(),
                () -> "Circular Maven business dependencies: "
                        + cycles
        );
    }

    private ScanResult scanProductionJava()
            throws IOException {

        Set<ModuleEdge> edges =
                new LinkedHashSet<>();

        List<String> violations =
                new ArrayList<>();

        for (String source : BUSINESS_MODULES) {

            Path javaRoot =
                    BACKEND_ROOT.resolve(
                            source + "/src/main/java"
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

                    String sourceText =
                            Files.readString(javaFile);

                    Matcher matcher =
                            IMPORT_PATTERN.matcher(sourceText);

                    while (matcher.find()) {
                        String imported =
                                matcher.group(1);

                        String target =
                                businessModuleOf(imported);

                        if (target == null
                                || target.equals(source)) {
                            continue;
                        }

                        ModuleEdge edge =
                                new ModuleEdge(
                                        source,
                                        target
                                );

                        edges.add(edge);

                        String violation =
                                validateImport(
                                        edge,
                                        imported
                                );

                        if (violation != null) {
                            violations.add(
                                    javaFile
                                            + " imports "
                                            + imported
                                            + ": "
                                            + violation
                            );
                        }
                    }
                }
            }
        }

        return new ScanResult(
                Set.copyOf(edges),
                List.copyOf(violations)
        );
    }

    private String validateImport(
            ModuleEdge edge,
            String imported
    ) {

        if (!APPROVED_EDGES.contains(edge)) {
            return "business edge is not explicitly approved";
        }

        if (containsForbiddenSurface(imported)) {
            return "implementation/internal surface is forbidden";
        }

        if ("security".equals(edge.target())) {
            if (EXACT_PUBLIC_SECURITY_CONTRACTS.contains(imported)) {
                return null;
            }

            for (String prefix : PUBLIC_SECURITY_PREFIXES) {
                if (imported.startsWith(prefix)) {
                    return null;
                }
            }

            return "Security surface is not part of "
                    + "the reviewed public contract";
        }

        return "edge has no explicit public-surface policy";
    }

    private boolean containsForbiddenSurface(
            String imported
    ) {

        String lower =
                imported.toLowerCase();

        return lower.contains(".infrastructure.")
                || lower.contains(".domain.repository.")
                || lower.contains(".application.port.out.")
                || lower.contains(".application.port.output.")
                || lower.contains(".configuration.")
                || lower.contains(".config.")
                || lower.contains(".entity.")
                || lower.contains("jpaentity")
                || lower.startsWith("com.sixpay.security.jwt.")
                || imported.equals(
                "com.sixpay.security.authentication."
                        + "SecurityContextCurrentUserProvider"
        );
    }

    private Set<ModuleEdge>
    scanMavenBusinessDependencies()
            throws Exception {

        Set<ModuleEdge> edges =
                new LinkedHashSet<>();

        var factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(false);

        for (String source : BUSINESS_MODULES) {

            Path pom =
                    BACKEND_ROOT.resolve(
                            source + "/pom.xml"
                    );

            if (!Files.isRegularFile(pom)) {
                continue;
            }

            var document =
                    factory.newDocumentBuilder()
                            .parse(pom.toFile());

            var dependencies =
                    document.getElementsByTagName(
                            "dependency"
                    );

            for (int i = 0;
                 i < dependencies.getLength();
                 i++) {

                var dependency =
                        dependencies.item(i);

                String groupId =
                        childText(
                                dependency,
                                "groupId"
                        );

                String artifactId =
                        childText(
                                dependency,
                                "artifactId"
                        );

                String scope =
                        childText(
                                dependency,
                                "scope"
                        );

                if (!"com.sixpay".equals(groupId)) {
                    continue;
                }

                if (!BUSINESS_MODULES.contains(artifactId)) {
                    continue;
                }

                if (artifactId.equals(source)) {
                    continue;
                }

                if ("test".equals(scope)) {
                    continue;
                }

                edges.add(
                        new ModuleEdge(
                                source,
                                artifactId
                        )
                );
            }
        }

        return Set.copyOf(edges);
    }

    private String childText(
            org.w3c.dom.Node parent,
            String name
    ) {

        var children =
                parent.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            var child =
                    children.item(i);

            if (name.equals(child.getNodeName())) {
                return child
                        .getTextContent()
                        .trim();
            }
        }

        return "";
    }

    private String businessModuleOf(
            String imported
    ) {

        String prefix =
                "com.sixpay.";

        if (!imported.startsWith(prefix)) {
            return null;
        }

        String remainder =
                imported.substring(prefix.length());

        int separator =
                remainder.indexOf('.');

        String candidate =
                separator >= 0
                        ? remainder.substring(0, separator)
                        : remainder;

        return BUSINESS_MODULES.contains(candidate)
                ? candidate
                : null;
    }

    private Map<String, Set<String>>
    adjacency(
            Set<ModuleEdge> edges
    ) {

        Map<String, Set<String>> graph =
                new LinkedHashMap<>();

        for (String module : BUSINESS_MODULES) {
            graph.put(
                    module,
                    new LinkedHashSet<>()
            );
        }

        for (ModuleEdge edge : edges) {
            graph.get(edge.source())
                    .add(edge.target());
        }

        return graph;
    }

    private List<String> findCycles(
            Map<String, Set<String>> graph
    ) {

        List<String> cycles =
                new ArrayList<>();

        Set<String> visited =
                new HashSet<>();

        Set<String> active =
                new HashSet<>();

        Deque<String> path =
                new ArrayDeque<>();

        for (String module : BUSINESS_MODULES) {
            detectCycle(
                    module,
                    graph,
                    visited,
                    active,
                    path,
                    cycles
            );
        }

        return cycles;
    }

    private void detectCycle(
            String current,
            Map<String, Set<String>> graph,
            Set<String> visited,
            Set<String> active,
            Deque<String> path,
            List<String> cycles
    ) {

        if (active.contains(current)) {
            List<String> cycle =
                    new ArrayList<>();

            boolean collect = false;

            for (String node : path) {
                if (node.equals(current)) {
                    collect = true;
                }

                if (collect) {
                    cycle.add(node);
                }
            }

            cycle.add(current);

            cycles.add(
                    String.join(
                            " -> ",
                            cycle
                    )
            );

            return;
        }

        if (!visited.add(current)) {
            return;
        }

        active.add(current);
        path.addLast(current);

        for (String target :
                graph.getOrDefault(
                        current,
                        Set.of()
                )) {

            detectCycle(
                    target,
                    graph,
                    visited,
                    active,
                    path,
                    cycles
            );
        }

        path.removeLast();
        active.remove(current);
    }

    private <T> Set<T> difference(
            Set<T> left,
            Set<T> right
    ) {

        Set<T> result =
                new LinkedHashSet<>(left);

        result.removeAll(right);

        return result;
    }

    private record ModuleEdge(
            String source,
            String target
    ) {
    }

    private record ScanResult(
            Set<ModuleEdge> edges,
            List<String> violations
    ) {
    }
}
