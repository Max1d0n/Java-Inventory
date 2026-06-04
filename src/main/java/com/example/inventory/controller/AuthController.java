package com.example.inventory.controller;

import com.example.inventory.service.JwtService;
import com.example.inventory.entity.User;
import com.example.inventory.repository.UserRepository;
import com.example.inventory.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "API for user authentication and profile management")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    private static final String RECAPTCHA_SECRET = "6LctEfEsAAAAAPBFvugAuHYU5P2bU62KIy4Zrb1O";
    private static final String RECAPTCHA_URL = "https://www.google.com/recaptcha/api/siteverify";

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    private boolean verifyCaptcha(String captchaToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = RECAPTCHA_URL + "?secret=" + RECAPTCHA_SECRET + "&response=" + captchaToken;
        Map response = restTemplate.postForObject(url, null, Map.class);
        return response != null && Boolean.TRUE.equals(response.get("success"));
    }

    @Operation(summary = "Register new user", description = "Creates a new user account with captcha verification")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("New user registration attempt: {}", request.getUsername());
        String role = request.getRole() != null ? request.getRole() : "ROLE_CUSTOMER";

        if (role.equals("ROLE_SUPPLIER") &&
                (request.getEmail() == null || request.getEmail().isBlank())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email is required for suppliers"));
        }

        if (!List.of("ROLE_CUSTOMER", "ROLE_SUPPLIER", "ROLE_ADMIN").contains(role)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid role"));
        }

        if (!verifyCaptcha(request.getCaptcha())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Captcha verification failed"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(role);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully"));
    }

    @Operation(summary = "Login", description = "Authenticates user and returns JWT token")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(null);
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole());
        return ResponseEntity.ok(new LoginResponse(token, user.getRole(), user.getUsername()));
    }

    @Operation(summary = "Get current user profile", description = "Returns profile data of the authenticated user")
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Update email", description = "Updates email address of the authenticated user")
    @PutMapping("/profile/email")
    public ResponseEntity<Map<String, String>> updateEmail(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody UpdateEmailRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmail(request.getEmail());
        userRepository.save(user);
        log.info("Email update requested for user: {}", username);
        return ResponseEntity.ok(Map.of("message", "Email updated successfully"));
    }

    @Operation(summary = "Update password", description = "Updates password of the authenticated user")
    @PutMapping("/profile/password")
    public ResponseEntity<Map<String, String>> updatePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdatePasswordRequest request) {
        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        log.info("Password update requested for user: {}", username);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}