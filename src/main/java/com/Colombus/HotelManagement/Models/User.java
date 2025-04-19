package com.Colombus.HotelManagement.Models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, unique = true)
    private String contactNumber;

    @Column(nullable = false, unique = true)
    private String mobileNumber;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String userName;

    @Column(nullable = false)
    private String password;  // Will be stored as a hashed value

    @Column(nullable = false)
    private String concerningPersonName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column
    private String website;

    @Column(nullable = false)
    private String role;
    
    @Column(nullable = false)
    private boolean approved = false; // Default is false until admin approves

}
