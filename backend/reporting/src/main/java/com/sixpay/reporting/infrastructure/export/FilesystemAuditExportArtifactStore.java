package com.sixpay.reporting.infrastructure.export;

import com.sixpay.reporting.application.port.output.AuditExportArtifactStore;
import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.configuration.ReportingAuditExportProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Objects;

@Component
public final class FilesystemAuditExportArtifactStore
        implements AuditExportArtifactStore {

    private final ReportingAuditExportProperties properties;

    public FilesystemAuditExportArtifactStore(
            ReportingAuditExportProperties properties
    ) {
        this.properties = Objects.requireNonNull(properties);
    }

    @Override
    public StoredAuditExportArtifact store(
            AuditExportJobDefinition job,
            GeneratedAuditExport generated
    ) {
        Path directory = properties.storageDirectory()
                .toAbsolutePath()
                .normalize();

        String extension =
                job.format().name().toLowerCase();

        Path target = directory.resolve(
                job.exportId() + "." + extension
        ).normalize();

        if (!target.startsWith(directory)) {
            throw new IllegalStateException(
                    "Invalid audit export target path"
            );
        }

        try {
            Files.createDirectories(directory);
            Files.move(
                    generated.temporaryFile(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            try {
                Files.move(
                        generated.temporaryFile(),
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException nested) {
                throw new IllegalStateException(
                        "Cannot store audit export artifact",
                        nested
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot store audit export artifact",
                    exception
            );
        }

        URI retrieval = properties.retrievalBaseUri()
                .resolve(
                        job.exportId() + "." + extension
                );

        return new StoredAuditExportArtifact(retrieval);
    }
}
