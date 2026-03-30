package com.chtrembl.petstore.product.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Converter for Product.Status enum
 * Converts between lowercase JSON values ("available") and uppercase database values ("AVAILABLE")
 * This allows JSON API to use lowercase while database uses uppercase enum constant names
 */
@Converter(autoApply = true)
public class StatusConverter implements AttributeConverter<Product.Status, String> {

    @Override
    public String convertToDatabaseColumn(Product.Status status) {
        if (status == null) {
            return null;
        }
        // Store uppercase enum constant name in database
        return status.name();
    }

    @Override
    public Product.Status convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        // Try uppercase first (database format: AVAILABLE, PENDING, SOLD)
        try {
            return Product.Status.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Fallback: try fromValue for lowercase (JSON format: available, pending, sold)
            return Product.Status.fromValue(dbData.toLowerCase());
        }
    }
}
