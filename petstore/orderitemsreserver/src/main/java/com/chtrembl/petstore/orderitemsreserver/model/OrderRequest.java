package com.chtrembl.petstore.orderitemsreserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest implements Serializable {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    private String username;
    private String userId;

    @NotEmpty(message = "Product list cannot be empty")
    private List<Product> products;
}
