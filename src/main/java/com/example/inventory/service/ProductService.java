package com.example.inventory.service;

import com.example.inventory.entity.Product;
import com.example.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAll() {
        log.info("Getting all products");
        return productRepository.findAll();
    }

    public Product getById(Long id) {
        log.info("Getting product by id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product create(Product product) {
        log.info("Creating product: {}", product.getName());
        return productRepository.save(product);
    }

    public Product update(Long id, Product product) {
        log.info("Updating product with id: {}", id);
        Product existing = getById(id);
        existing.setName(product.getName());
        existing.setCategory(product.getCategory());
        existing.setQuantity(product.getQuantity());
        existing.setPrice(product.getPrice());
        existing.setSupplier(product.getSupplier());
        existing.setImageUrl(product.getImageUrl());
        existing.setDescription(product.getDescription());
        return productRepository.save(existing);
    }

    public void delete(Long id) {
        log.info("Deleting product with id: {}", id);
        productRepository.deleteById(id);
    }
}