package com.chtrembl.petstore.product.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated This class is no longer used. Product data is now loaded from PostgreSQL database.
 * Data initialization is handled by src/main/resources/data.sql.
 * This class remains for backward compatibility but will be removed in future versions.
 * See POSTGRESQL_MIGRATION.md for details.
 */
@Deprecated(since = "0.0.1-SNAPSHOT", forRemoval = true)
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("data")
@Data
public class DataPreload {
    private List<Product> products = new ArrayList<>();
}
