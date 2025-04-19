package com.Colombus.HotelManagement.Controllers;

import com.Colombus.HotelManagement.Models.User;
import com.Colombus.HotelManagement.Security.JwtUtil;
import com.Colombus.HotelManagement.Services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        try {
            logger.info("Registration attempt for user: {}", user.getUserName());
            
            // Validation checks
            if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
            }
            
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
            }
            
            // Check if user already exists
            if (userService.getUserByUserName(user.getUserName()).isPresent()) {
                logger.warn("Registration failed: Username already exists: {}", user.getUserName());
                return ResponseEntity.badRequest().body(Map.of("message", "Username already exists"));
            }
            
            // Ensure role is set properly
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("USER");
            }
            
            // For first user, set as ADMIN and approved
            long userCount = userService.getUserCount();
            if (userCount == 0) {
                user.setRole("ADMIN");
                user.setApproved(true);
                logger.info("First user - setting as ADMIN and approved");
            } else {
                // Other users need approval
                user.setApproved(false);
            }
            
            // Log registration details
            logger.info("Creating user: {}, role: {}, approved: {}", 
                    user.getUserName(), user.getRole(), user.isApproved());
            
            User registeredUser = userService.registerUser(user);
            
            logger.info("User registered successfully: {}", registeredUser.getUserName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Registration successful! " + 
                    (registeredUser.isApproved() ? "You can now login." : "Your account is pending admin approval."));
            
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", registeredUser.getId());
            userMap.put("userName", registeredUser.getUserName());
            userMap.put("role", registeredUser.getRole());
            userMap.put("email", registeredUser.getEmail());
            userMap.put("approved", registeredUser.isApproved());
            
            response.put("user", userMap);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        try {
        String userName = credentials.get("userName");
        String password = credentials.get("password");

            logger.info("Login attempt for user: {}", userName);

            if (userName == null || password == null) {
                logger.warn("Login failed: Username or password is null");
                return ResponseEntity.badRequest().body(Map.of("message", "Username and password are required"));
            }

            // Special case for admin user if it's not in the database
            if ("ADMIN1".equals(userName) && "password".equals(password)) {
                logger.info("Admin login detected");
                User adminUser = new User();
                adminUser.setId(999L); // Use a special ID for the admin
                adminUser.setUserName("ADMIN1");
                adminUser.setRole("ADMIN"); // Note: not prefixed with ROLE_ (that's added in the filter)
                adminUser.setEmail("admin@example.com");
                adminUser.setApproved(true); // Admin is always approved
                
                String token = jwtUtil.generateToken(adminUser);
                logger.info("Generated token for admin: {}", token);

                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", adminUser.getId());
                userMap.put("userName", adminUser.getUserName());
                userMap.put("role", adminUser.getRole());
                userMap.put("email", adminUser.getEmail());
                userMap.put("approved", adminUser.isApproved());
                
                response.put("user", userMap);
                
                logger.info("Admin login response: {}", response);

                return ResponseEntity.ok(response);
            }

            // Find user in database
            Optional<User> userOpt = userService.getUserByUserName(userName);
            if (userOpt.isEmpty()) {
                logger.warn("Login failed: User '{}' does not exist", userName);
                return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
            }
            
            User user = userOpt.get();
            
            // Check password
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("Login failed: Invalid password for user '{}'", userName);
                return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password"));
            }
            
            // Check approval status for non-admin users
            if (!"ADMIN".equals(user.getRole()) && !user.isApproved()) {
                logger.warn("Login failed: User '{}' is not approved", userName);
                return ResponseEntity.status(403).body(Map.of("message", "Your account is pending approval by an administrator."));
            }
            
            // User is valid, generate token
            String token = jwtUtil.generateToken(user);
            logger.info("Generated token for user: {}", userName);

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("userName", user.getUserName());
            userMap.put("role", user.getRole());
            userMap.put("email", user.getEmail());
            userMap.put("approved", user.isApproved());
                
            response.put("user", userMap);
                
            logger.info("Login successful for user: {}", userName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Login error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                
                logger.info("Token validation - Username: {}, Role: {}", username, role);
                
                if (username != null && role != null) {
                    return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "username", username,
                        "role", role
                    ));
                }
            }
            logger.warn("Invalid token format or missing data");
            return ResponseEntity.status(401).body(Map.of("message", "Invalid token"));
        } catch (Exception e) {
            logger.error("Token validation error: {}", e.getMessage(), e);
            return ResponseEntity.status(401).body(Map.of("message", "Token validation failed"));
        }
    }
    
    // Get pending approval users (admin only)
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getPendingApprovals() {
        try {
            List<User> pendingUsers = userService.getPendingApprovalUsers();
            
            // Convert to a list of maps with selected user properties
            List<Map<String, Object>> usersResponse = pendingUsers.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("userName", user.getUserName());
                    userMap.put("email", user.getEmail());
                    userMap.put("companyName", user.getCompanyName());
                    userMap.put("contactNumber", user.getContactNumber());
                    userMap.put("mobileNumber", user.getMobileNumber());
                    userMap.put("address", user.getAddress());
                    userMap.put("city", user.getCity());
                    userMap.put("state", user.getState());
                    userMap.put("website", user.getWebsite());
                    userMap.put("concerningPersonName", user.getConcerningPersonName());
                    return userMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(usersResponse);
        } catch (Exception e) {
            logger.error("Error fetching pending approvals: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to fetch pending approvals: " + e.getMessage()));
        }
    }
    
    // Approve user (admin only)
    @PostMapping("/approve-user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        try {
            logger.info("Attempting to approve user with ID: {}", userId);
            
            // First fetch the user to verify it exists
            Optional<User> userOptional = userService.getUserById(userId);
            if (userOptional.isEmpty()) {
                logger.warn("User with ID {} not found for approval", userId);
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            
            User user = userOptional.get();
            logger.info("Found user for approval: {} (current approval status: {})", 
                    user.getUserName(), user.isApproved());
            
            // Set to approved and save
            user.setApproved(true);
            User approvedUser = userService.saveUser(user);
            
            logger.info("User {} successfully approved", approvedUser.getUserName());
            
            return ResponseEntity.ok(Map.of(
                "message", "User approved successfully",
                "user", Map.of(
                    "id", approvedUser.getId(),
                    "userName", approvedUser.getUserName(),
                    "email", approvedUser.getEmail(),
                    "approved", approvedUser.isApproved()
                )
            ));
        } catch (Exception e) {
            logger.error("Error approving user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to approve user: " + e.getMessage()));
        }
    }
    
    // Reject/delete user (admin only)
    @DeleteMapping("/reject-user/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        try {
            userService.rejectUser(userId);
            return ResponseEntity.ok(Map.of("message", "User rejected and removed successfully"));
        } catch (Exception e) {
            logger.error("Error rejecting user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to reject user: " + e.getMessage()));
        }
    }

    // Debug endpoint to check user status
    @GetMapping("/check-user/{userName}")
    public ResponseEntity<?> checkUserStatus(@PathVariable String userName) {
        try {
            Optional<User> userOpt = userService.getUserByUserName(userName);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return ResponseEntity.ok(Map.of(
                    "exists", true,
                    "userName", user.getUserName(),
                    "role", user.getRole(),
                    "approved", user.isApproved(),
                    "passwordHash", user.getPassword().substring(0, 10) + "..." // Show part of hash for debugging
                ));
            } else {
                return ResponseEntity.ok(Map.of("exists", false));
            }
        } catch (Exception e) {
            logger.error("Error checking user status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get all users (admin only)
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        try {
            logger.info("Fetching all users");
            List<User> users = userService.getAllUsers();
            
            List<Map<String, Object>> usersResponse = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("userName", user.getUserName());
                    userMap.put("email", user.getEmail());
                    userMap.put("companyName", user.getCompanyName());
                    userMap.put("contactNumber", user.getContactNumber());
                    userMap.put("mobileNumber", user.getMobileNumber());
                    userMap.put("address", user.getAddress());
                    userMap.put("city", user.getCity());
                    userMap.put("state", user.getState());
                    userMap.put("website", user.getWebsite());
                    userMap.put("concerningPersonName", user.getConcerningPersonName());
                    userMap.put("role", user.getRole());
                    userMap.put("approved", user.isApproved());
                    return userMap;
                })
                .collect(Collectors.toList());
            
            logger.info("Returning {} users", usersResponse.size());
            return ResponseEntity.ok(usersResponse);
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("message", "Failed to fetch users: " + e.getMessage()));
        }
    }

    // Download users as CSV (admin only)
    @GetMapping("/users/download-csv")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<byte[]> downloadUsersAsCsv() {
        try {
            logger.info("Generating user data CSV");
            List<User> users = userService.getAllUsers();
            
            StringBuilder csvContent = new StringBuilder();
            // Add CSV header
            csvContent.append("ID,Username,Email,Company Name,Contact Person,Contact Number,Mobile Number,Address,City,State,Website,Role,Approved\n");
            
            // Add data rows
            for (User user : users) {
                csvContent.append(user.getId()).append(",");
                csvContent.append(escapeCsvField(user.getUserName())).append(",");
                csvContent.append(escapeCsvField(user.getEmail())).append(",");
                csvContent.append(escapeCsvField(user.getCompanyName())).append(",");
                csvContent.append(escapeCsvField(user.getConcerningPersonName())).append(",");
                csvContent.append(escapeCsvField(user.getContactNumber())).append(",");
                csvContent.append(escapeCsvField(user.getMobileNumber())).append(",");
                csvContent.append(escapeCsvField(user.getAddress())).append(",");
                csvContent.append(escapeCsvField(user.getCity())).append(",");
                csvContent.append(escapeCsvField(user.getState())).append(",");
                csvContent.append(escapeCsvField(user.getWebsite())).append(",");
                csvContent.append(escapeCsvField(user.getRole())).append(",");
                csvContent.append(user.isApproved()).append("\n");
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "travel_agents.csv");
            
            logger.info("CSV generated successfully with {} users", users.size());
            return ResponseEntity
                .ok()
                .headers(headers)
                .body(csvContent.toString().getBytes());
        } catch (Exception e) {
            logger.error("Error generating CSV: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Helper method to escape CSV fields that may contain commas
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        
        // If the field contains commas, quotes, or newlines, wrap it in quotes and escape any quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
}
