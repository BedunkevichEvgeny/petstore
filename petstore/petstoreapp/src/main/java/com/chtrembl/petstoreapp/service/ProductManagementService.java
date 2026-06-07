package com.chtrembl.petstoreapp.service;

import com.chtrembl.petstoreapp.client.ProductServiceClient;
import com.chtrembl.petstoreapp.exception.ProductServiceException;
import com.chtrembl.petstoreapp.model.ContainerEnvironment;
import com.chtrembl.petstoreapp.model.Product;
import com.chtrembl.petstoreapp.model.Tag;
import com.chtrembl.petstoreapp.model.User;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.chtrembl.petstoreapp.config.Constants.CATEGORY;
import static com.chtrembl.petstoreapp.config.Constants.OPERATION;
import static com.chtrembl.petstoreapp.config.Constants.REQUEST_ID;
import static com.chtrembl.petstoreapp.config.Constants.TRACE_ID;
import static com.chtrembl.petstoreapp.model.Status.AVAILABLE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductManagementService {

    private final User sessionUser;
    private final ContainerEnvironment containerEnvironment;
    private final ProductServiceClient productServiceClient;

    public Collection<Product> getProductsByCategory(String category, List<Tag> tags) {
        List<Product> products;

        MDC.put(OPERATION, "getProducts");
        MDC.put(CATEGORY, category);

        String requestId = MDC.get(REQUEST_ID);
        String traceId = MDC.get(TRACE_ID);

        System.out.println("Starting product retrieval...");

        try {
            this.sessionUser.getTelemetryClient().trackEvent(
                    String.format("PetStoreApp user %s is requesting to retrieve products from the ProductService",
                            this.sessionUser.getName()),
                    this.sessionUser.getCustomEventProperties(), null);

            products = productServiceClient.getProductsByStatus(AVAILABLE.getValue());
            this.sessionUser.setProducts(products);

            if (tags.get(0).getName().equals("large")) {
                products = products.stream()
                        // Reversed equals to make it vulnerable to NullPointerException
                        .filter(product -> product.getCategory().getName().equals(category)
                                && product.getTags().toString().contains("large"))
                        .toList();
            } else {
                products = products.stream()
                        .filter(product -> product.getCategory().getName().equals(category)
                                && product.getTags().toString().contains("small"))
                        .toList();
            }

            return products;
        } catch (Exception fe) {
            fe.printStackTrace();
            return null;
        }
    }
}
