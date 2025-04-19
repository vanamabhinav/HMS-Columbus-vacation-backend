package com.Colombus.HotelManagement.Controllers;

import com.Colombus.HotelManagement.Models.User;
import com.Colombus.HotelManagement.Services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/users")
//@CrossOrigin(origins="*")
@CrossOrigin(origins = "http://localhost:3000") // Allow frontend access
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get user details by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
