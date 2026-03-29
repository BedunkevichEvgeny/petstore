package com.chtrembl.petstore.pureorderitemsreserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest implements Serializable {
    private String sessionId;
    private String username;
    private String userId;
    private List<Product> products;
}
