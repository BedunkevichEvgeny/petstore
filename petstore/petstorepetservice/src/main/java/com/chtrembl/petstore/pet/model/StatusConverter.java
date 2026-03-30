package com.chtrembl.petstore.pet.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for Pet.Status enum
 * Converts between lowercase JSON values ("available") and uppercase database values ("AVAILABLE")
 * This allows JSON API to use lowercase while database uses uppercase enum constant names
 */
@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Pet.Status, String> {

    @Override
    public String convertToDatabaseColumn(Pet.Status status) {
        if (status == null) {
            return null;
        }
        // Store uppercase enum constant name in database
        return status.name();
    }

    @Override
    public Pet.Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        // Try uppercase first (database format: AVAILABLE, PENDING, SOLD)
        try {
            return Pet.Status.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback: try fromValue for lowercase (JSON format: available, pending, sold)
            return Pet.Status.fromValue(dbData.toLowerCase());
        }
    }
}
