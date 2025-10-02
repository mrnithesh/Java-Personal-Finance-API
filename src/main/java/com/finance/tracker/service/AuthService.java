package com.finance.tracker.service;

import com.finance.tracker.dto.AuthResponse;
import com.finance.tracker.dto.LoginRequest;
import com.finance.tracker.dto.RegisterRequest;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.UserRepository;
import com.finance.tracker.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service
 * 
 * Handles user registration and login
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Register a new user
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        // Save user to database
        userRepository.save(user);

        // Load user details for token generation
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        
        // Generate JWT token
        String jwtToken = jwtUtil.generateToken(userDetails);

        // Return response
        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .message("User registered successfully")
                .build();
    }

    /**
     * Authenticate user and generate JWT token
     */
    public AuthResponse login(LoginRequest request) {
        
        // Authenticate user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        
        // Get user from database for additional info
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        String jwtToken = jwtUtil.generateToken(userDetails);

        // Return response
        return AuthResponse.builder()
                .token(jwtToken)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .message("Login successful")
                .build();
    }
}
