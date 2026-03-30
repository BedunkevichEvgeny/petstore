package com.chtrembl.petstore.pet.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pet")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Valid
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull
    private String name;

    @JsonProperty("photoURL")
    @NotNull
    private String photoURL;

    @Valid
    @Builder.Default
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinTable(
        name = "pet_tag",
        joinColumns = @JoinColumn(name = "pet_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private List<Tag> tags = new ArrayList<>();

    @Convert(converter = StatusConverter.class)
    private Status status;

    public Pet name(String name) {
        this.name = name;
        return this;
    }

    public enum Status {
        AVAILABLE("available"),
        PENDING("pending"),
        SOLD("sold");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        /**
         * Get Status enum from JSON value (lowercase: "available", "pending", "sold")
         * Used by Jackson for JSON deserialization
         */
        @JsonCreator
        public static Status fromValue(String value) {
            if (value == null || value.isEmpty()) {
                throw new IllegalArgumentException("Status value cannot be null or empty");
            }

            for (Status status : Status.values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unexpected value '" + value + "'. Expected: available, pending, sold");
        }

        /**
         * Case-insensitive valueOf that handles both formats:
         * - Enum constant name: "AVAILABLE", "PENDING", "SOLD"
         * - JSON value: "available", "pending", "sold"
         *
         * @param input The status string (case-insensitive)
         * @return The matching Status enum
         * @throws IllegalArgumentException if no match found
         */
        public static Status valueOfIgnoreCase(String input) {
            if (input == null || input.isEmpty()) {
                throw new IllegalArgumentException("Status value cannot be null or empty");
            }

            // Try enum constant name first (AVAILABLE, PENDING, SOLD)
            try {
                return Status.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Fallback: try JSON value format (available, pending, sold)
                return fromValue(input.toLowerCase());
            }
        }

        /**
         * Get JSON value (lowercase: "available", "pending", "sold")
         * Used by Jackson for JSON serialization
         */
        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
