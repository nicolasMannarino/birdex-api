package com.birdex.entity.converter;

import com.birdex.entity.enums.ReportStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReportStatus attribute) {
        return attribute == null ? null : attribute.name().toLowerCase();
    }

    @Override
    public ReportStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReportStatus.valueOf(dbData.toUpperCase());
    }
}
