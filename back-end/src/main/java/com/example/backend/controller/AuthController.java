package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000") // dovoli frontend na portu 3000
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Registracija
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody User user) {
        // preveri, če uporabnik že obstaja po emailu
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT) // 409 Conflict
                    .body(Map.of("message", "Email already in use"));
        }

        // shrani novega uporabnika
        userRepository.save(user);

        return ResponseEntity
                .status(HttpStatus.CREATED) // 201 Created
                .body(Map.of("message", "User registered successfully"));
    }

    // Prijava
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody User user) {
        Optional<User> existingOpt = userRepository.findByEmail(user.getEmail());

        if (existingOpt.isEmpty()) {
            // uporabnik ne obstaja
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                    .body(Map.of("message", "Invalid credentials"));
        }

        User existingUser = existingOpt.get();

        if (!existingUser.getPassword().equals(user.getPassword())) {
            // geslo napačno
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials"));
        }

        // prijava uspešna
        return ResponseEntity
                .ok(Map.of("message", "Login successful"));
    }
}
