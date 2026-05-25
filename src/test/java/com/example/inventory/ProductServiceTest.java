package com.example.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllProducts_returnsAllProducts() {
        Product product = new Product();
        product.setName("Laptop");
        product.setCategory("Electronics");
        product.setQuantity(10);
        product.setPrice(999.99);

        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.getAll();

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void getById_existingId_returnsProduct() {
        Product product = new Product();
        product.setName("Laptop");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getById(1L);

        assertEquals("Laptop", result.getName());
    }

    @Test
    void getById_nonExistingId_throwsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getById(999L));
    }
}