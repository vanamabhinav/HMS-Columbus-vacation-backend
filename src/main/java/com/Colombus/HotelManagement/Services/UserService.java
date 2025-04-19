package com.Colombus.HotelManagement.Services;

import com.Colombus.HotelManagement.Models.User;
import com.Colombus.HotelManagement.Repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, @Lazy PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a new user
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }
        if (userRepository.existsByUserName(user.getUserName())) {
            throw new RuntimeException("Username already taken!");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword())); // Hash password

        // Assign role dynamically: If first user, make admin; otherwise, user
        if (userRepository.count() == 0) {
            user.setRole("ADMIN");
            user.setApproved(true); // Auto-approve first user (admin)
        } else {
            user.setRole("USER");
            user.setApproved(false); // New users need approval
        }
        return userRepository.save(user);
    }

    // Authenticate user
    public Optional<User> authenticateUser(String userName, String password) {
        Optional<User> user = userRepository.findByUserName(userName);
        
        if (user.isEmpty()) {
            logger.warn("User not found: {}", userName);
            return Optional.empty();
        }
        
        User foundUser = user.get();
        boolean passwordMatches = passwordEncoder.matches(password, foundUser.getPassword());
        
        logger.info("Authentication attempt - User: {}, PasswordMatches: {}, Role: {}, Approved: {}", 
                foundUser.getUserName(), 
                passwordMatches, 
                foundUser.getRole(),
                foundUser.isApproved());
        
        // First check if user exists and password matches
        if (passwordMatches) {
            // For admins, always allow login regardless of approval status
            if ("ADMIN".equals(foundUser.getRole())) {
                logger.info("Admin login successful: {}", userName);
                return user;
            }
            
            // For regular users, check approval status
            if (foundUser.isApproved()) {
                logger.info("Approved user login successful: {}", userName);
                return user;
            }
            
            // User exists and password is correct, but not approved
            logger.warn("User {} attempted to login but is not approved", userName);
        } else {
            logger.warn("Password does not match for user: {}", userName);
        }
        
        return Optional.empty();
    }

    // Get user by ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    
    // Get user by username
    public Optional<User> getUserByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }
    
    // Get all pending approval users
    public List<User> getPendingApprovalUsers() {
        return userRepository.findByApprovedFalse();
    }
    
    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    // Get user count
    public long getUserCount() {
        return userRepository.count();
    }
    
    // Approve user
    public User approveUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setApproved(true);
        return userRepository.save(user);
    }
    
    // Save user
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    // Reject/delete user
    public void rejectUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
