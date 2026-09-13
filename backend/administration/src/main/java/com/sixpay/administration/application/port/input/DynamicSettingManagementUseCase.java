package com.sixpay.administration.application.port.input;

import com.sixpay.administration.domain.model.DynamicSettingHistoryEntry;
import com.sixpay.administration.domain.model.DynamicSettingValue;
import com.sixpay.administration.domain.model.SettingDefinition;
import java.util.List;

public interface DynamicSettingManagementUseCase {
    List<SettingDefinition> definitions();
    DynamicSettingValue get(String key);
    DynamicSettingValue update(String key, String value, String reason, String actor);
    List<DynamicSettingHistoryEntry> history(String key);
    DynamicSettingValue rollback(String key, long targetVersion, String reason, String actor);
}
