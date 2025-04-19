package com.Colombus.HotelManagement.Services;

import com.Colombus.HotelManagement.Models.User;
import com.Colombus.HotelManagement.Repositories.UserRepository;
import com.Colombus.HotelManagement.Security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    // User Registration
    public User registerUser(User user) {
        if (userRepository.findByUserName(user.getUserName()).isPresent()) {
            throw new RuntimeException("Username already exists!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Hash password
        return userRepository.save(user);
    }

    // User Login
    public String loginUser(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        return jwtUtil.generateToken(user); // Now passing User directly
    }

}
