package com.Colombus.HotelManagement.Models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name="hotels")
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String hotelName;

    @Column(nullable = false, unique = true)
    private String email1;

    @Column(unique = true)
    private String email2;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String mobilePhoneContact;

    @Column(unique = true)
    private String landlineContact;

    @Column(nullable = false)
    private String concerningPersonName;

    private boolean preferred;  // To mark preferred hotels

    @Column
    private String website;
    
    @Column(nullable = false)
    private String city;
    
    @Column(nullable = false)
    private String state;

    // Getters and Setters
    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
