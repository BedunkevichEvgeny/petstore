package com.chtrembl.petstore.product.service;

import com.chtrembl.petstore.product.model.Product;
import com.chtrembl.petstore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findProductsByStatus(List<String> statusStrings) {
        log.info("Finding products with status: {}", statusStrings);

        // Convert string status values to Status enum (handles both "available" and "AVAILABLE")
        List<Product.Status> statuses = statusStrings.stream()
                .map(this::parseStatus)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        if (statuses.isEmpty()) {
            log.warn("No valid status values found in: {}", statusStrings);
            return List.of();
        }

        return productRepository.findByStatusIn(statuses);
    }

    public Optional<Product> findProductById(Long productId) {
        log.info("Finding product with id: {}", productId);
        return productRepository.findById(productId);
    }

    public List<Product> getAllProducts() {
        log.info("Getting all products");
        return productRepository.findAll();
    }

    public int getProductCount() {
        return (int) productRepository.count();
    }

    /**
     * Parse status string to Status enum.
     * Supports both lowercase ("available") and uppercase ("AVAILABLE") formats.
     */
    private Optional<Product.Status> parseStatus(String statusString) {
        if (statusString == null || statusString.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Product.Status.valueOfIgnoreCase(statusString));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value: '{}'. Expected: available, pending, sold (case-insensitive)", statusString);
            return Optional.empty();
        }
    }
}