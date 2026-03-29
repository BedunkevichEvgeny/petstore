package com.chtrembl.petstore.orderitemsreserver.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {
    private String id;
    private String name;
    private Integer quantity;
    private Double price;
}
