package com.sixpay.administration.application.port.output;

import com.sixpay.administration.domain.model.DynamicSettingHistoryEntry;
import java.util.List;
import java.util.Optional;

public interface DynamicSettingHistoryRepositoryPort {
    DynamicSettingHistoryEntry append(DynamicSettingHistoryEntry entry);
    List<DynamicSettingHistoryEntry> findByKeyOrderByNewVersionDesc(String key);
    Optional<DynamicSettingHistoryEntry> findByKeyAndNewVersion(String key, long version);
}
