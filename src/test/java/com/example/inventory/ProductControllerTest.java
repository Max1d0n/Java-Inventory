package com.example.inventory;

import com.example.inventory.entity.Product;
import com.example.inventory.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class ProductControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ProductRepository productRepository;

    private ObjectMapper objectMapper =  new ObjectMapper();

    private String adminToken;
    private String supplierToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
        adminToken = getToken("admin", "admin123");
        supplierToken = getToken("MadidonCo", "88005553535");
        customerToken = getToken("Madidon", "88005553535");
    }

    private String getToken(String username, String password) throws Exception {
        String body = """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    // ===== GET ALL PRODUCTS =====

    @Test
    void getAllProducts_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());
    }

    // ===== GET PRODUCT BY ID =====

    @Test
    void getProductById_existingId_returns200() throws Exception {
        Product product = productRepository.findAll().get(0);
        mockMvc.perform(get("/api/products/" + product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()));
    }

    @Test
    void getProductById_nonExistingId_returns404() throws Exception {
        mockMvc.perform(get("/api/products/99999"))
                .andExpect(status().isNotFound());
    }

    // ===== CREATE PRODUCT =====

    @Test
    void createProduct_asSupplier_returns200() throws Exception {
        String body = """
                {
                    "name": "Test Product",
                    "category": "Test",
                    "price": 99.99,
                    "quantity": 10,
                    "supplier": "Test Supplier"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + supplierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void createProduct_asCustomer_returns403() throws Exception {
        String body = """
                {
                    "name": "Test Product",
                    "category": "Test",
                    "price": 99.99,
                    "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_noAuth_returns403() throws Exception {
        String body = """
                {
                    "name": "Test Product",
                    "category": "Test",
                    "price": 99.99,
                    "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_emptyName_returns400() throws Exception {
        String body = """
                {
                    "name": "",
                    "category": "Test",
                    "price": 99.99,
                    "quantity": 10
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + supplierToken))
                .andExpect(status().isBadRequest());
    }

    // ===== UPDATE PRODUCT =====

    @Test
    void updateProduct_asSupplier_returns200() throws Exception {
        Product product = productRepository.findAll().get(0);
        String body = """
                {
                    "name": "Updated Product",
                    "category": "Updated",
                    "price": 199.99,
                    "quantity": 5
                }
                """;

        mockMvc.perform(put("/api/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + supplierToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Product"));
    }

    @Test
    void updateProduct_asCustomer_returns403() throws Exception {
        Product product = productRepository.findAll().get(0);
        String body = """
                {
                    "name": "Updated Product",
                    "category": "Updated",
                    "price": 199.99,
                    "quantity": 5
                }
                """;

        mockMvc.perform(put("/api/products/" + product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    // ===== DELETE PRODUCT =====

    @Test
    void deleteProduct_asAdmin_returns200() throws Exception {
        Product product = new Product();
        product.setName("To Delete");
        product.setCategory("Test");
        product.setPrice(9.99);
        product.setQuantity(1);
        product = productRepository.save(product);

        mockMvc.perform(delete("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void deleteProduct_asSupplier_returns403() throws Exception {
        Product product = productRepository.findAll().get(0);

        mockMvc.perform(delete("/api/products/" + product.getId())
                        .header("Authorization", "Bearer " + supplierToken))
                .andExpect(status().isForbidden());
    }
}