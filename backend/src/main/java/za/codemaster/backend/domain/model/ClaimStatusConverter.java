package za.codemaster.backend.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link ClaimStatus} to/from the lowercase strings the {@code claims.status}
 * database check constraint requires ({@code CHECK (status IN ('active', 'released', 'completed'))},
 * V4__comments_and_claims.sql).
 * <p>
 * {@code @Enumerated(EnumType.STRING)} would store the Java constant name ({@code "ACTIVE"})
 * instead of the lowercase wire/DB value ({@code "active"}) — the constraint rejects that
 * outright. {@code autoApply = true} means every {@link ClaimStatus}-typed field (currently
 * just {@link Claim#getStatus()}) uses this converter without needing an explicit
 * {@code @Convert} annotation.
 */
@Converter(autoApply = true)
public class ClaimStatusConverter implements AttributeConverter<ClaimStatus, String> {

    @Override
    public String convertToDatabaseColumn(ClaimStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ClaimStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ClaimStatus.fromValue(dbData);
    }
}
