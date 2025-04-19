package com.Colombus.HotelManagement.Controllers;

import com.Colombus.HotelManagement.Models.Hotel;
import com.Colombus.HotelManagement.Services.HotelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/hotels")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class HotelController {
    private static final Logger logger = LoggerFactory.getLogger(HotelController.class);
    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    // Add a new hotel (Admin only)
    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> addHotel(@RequestBody Hotel hotel, @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Received request to add hotel: {}", hotel);
            logger.info("Authorization header: {}", authHeader);
            
            // Extract and log token info
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                logger.info("Token first 20 chars: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            }
            
            // Print out all hotel properties for debugging
            logger.info("Hotel details - Name: {}, Email1: {}, Email2: {}, Address: {}, MobilePhone: {}, Landline: {}, ConcerningPerson: {}, Preferred: {}",
                hotel.getHotelName(),
                hotel.getEmail1(),
                hotel.getEmail2(),
                hotel.getAddress(),
                hotel.getMobilePhoneContact(),
                hotel.getLandlineContact(),
                hotel.getConcerningPersonName(),
                hotel.isPreferred()
            );
            
            // Validate required fields
            if (hotel.getHotelName() == null || hotel.getHotelName().trim().isEmpty()) {
                logger.warn("Hotel name is required but was empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Hotel name is required"));
            }
            if (hotel.getEmail1() == null || hotel.getEmail1().trim().isEmpty()) {
                logger.warn("Primary email is required but was empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Primary email is required"));
            }
            if (hotel.getMobilePhoneContact() == null || hotel.getMobilePhoneContact().trim().isEmpty()) {
                logger.warn("Mobile phone contact is required but was empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Mobile phone contact is required"));
            }
            if (hotel.getAddress() == null || hotel.getAddress().trim().isEmpty()) {
                logger.warn("Address is required but was empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Address is required"));
            }
            if (hotel.getConcerningPersonName() == null || hotel.getConcerningPersonName().trim().isEmpty()) {
                logger.warn("Concerning person name is required but was empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Concerning person name is required"));
            }

            logger.info("Attempting to save hotel to database");
            Hotel savedHotel = hotelService.addHotel(hotel);
            logger.info("Successfully added hotel with ID: {}", savedHotel.getId());
            return ResponseEntity.ok(savedHotel);
        } catch (Exception e) {
            logger.error("Error adding hotel: {}", e.getMessage(), e);
            // Print full stack trace for detailed debugging
            e.printStackTrace();
            
            if (e.getMessage() != null && 
                (e.getMessage().contains("ConstraintViolationException") || 
                e.getMessage().contains("DataIntegrityViolationException"))) {
                
                logger.warn("Constraint violation detected - likely duplicate entry");
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "A hotel with these details already exists. Please check unique fields (hotel name, emails, phone numbers)"));
            }
            
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to add hotel: " + e.getMessage()));
        }
    }

    // Get all hotels
    @GetMapping("/all")
    public ResponseEntity<List<Hotel>> getAllHotels() {
        logger.info("Fetching all hotels");
        List<Hotel> hotels = hotelService.getAllHotels();
        logger.info("Retrieved {} hotels", hotels.size());
        return ResponseEntity.ok(hotels);
    }

    // Search hotels by name
    @GetMapping("/search")
    public ResponseEntity<List<Hotel>> searchHotels(@RequestParam String name) {
        logger.info("Searching hotels with name: {}", name);
        List<Hotel> hotels = hotelService.searchHotelsByName(name);
        logger.info("Found {} hotels matching search criteria", hotels.size());
        return ResponseEntity.ok(hotels);
    }

    // Get preferred hotels
    @GetMapping("/preferred")
    public ResponseEntity<List<Hotel>> getPreferredHotels() {
        logger.info("Fetching preferred hotels");
        List<Hotel> hotels = hotelService.getPreferredHotels();
        logger.info("Retrieved {} preferred hotels", hotels.size());
        return ResponseEntity.ok(hotels);
    }

    // Get hotel by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getHotelById(@PathVariable Long id) {
        logger.info("Fetching hotel with ID: {}", id);
        Optional<Hotel> hotel = hotelService.getHotelById(id);
        
        if (hotel.isPresent()) {
            logger.info("Found hotel: {}", hotel.get().getHotelName());
            return ResponseEntity.ok(hotel.get());
        } else {
            logger.warn("Hotel with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    // Update hotel details (Admin only)
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @RequestBody Hotel updatedHotel) {
        try {
            return ResponseEntity.ok(hotelService.updateHotel(id, updatedHotel));
        } catch (RuntimeException e) {
            logger.error("Error updating hotel: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        }
    }

    // Delete hotel (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        try {
            hotelService.deleteHotel(id);
            return ResponseEntity.ok()
                .body(Map.of("message", "Hotel deleted successfully"));
        } catch (Exception e) {
            logger.error("Error deleting hotel: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Failed to delete hotel: " + e.getMessage()));
        }
    }

    @GetMapping("/search/city")
    public List<Hotel> searchHotelsByCity(@RequestParam String city) {
        return hotelService.searchHotelsByCity(city);
    }

    @GetMapping("/search/state")
    public List<Hotel> searchHotelsByState(@RequestParam String state) {
        return hotelService.searchHotelsByState(state);
    }

    @GetMapping("/search/location")
    public List<Hotel> searchHotelsByCityAndState(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state) {
        if (city != null && state != null) {
            return hotelService.searchHotelsByCityAndState(city, state);
        } else if (city != null) {
            return hotelService.searchHotelsByCity(city);
        } else if (state != null) {
            return hotelService.searchHotelsByState(state);
        }
        return hotelService.getAllHotels();
    }

    // Add multiple hotels via CSV file (Admin only)
    @PostMapping("/upload-csv")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> uploadCSV(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String authHeader) {
        try {
            logger.info("Received CSV upload request with file: {}, size: {}, content type: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());
            
            // Log auth info for debugging
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                logger.info("Auth token first 20 chars: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            }
            
            if (file.isEmpty()) {
                logger.warn("Uploaded file is empty");
                return ResponseEntity.badRequest().body(Map.of("message", "Please select a file to upload"));
            }

            // Check content type more flexibly
            String contentType = file.getContentType();
            logger.info("File content type: {}", contentType);
            
            if (contentType == null || !(contentType.equals("text/csv") || contentType.equals("application/vnd.ms-excel") || 
                contentType.equals("text/plain") || contentType.contains("csv"))) {
                logger.warn("Invalid file type: {}", contentType);
                return ResponseEntity.badRequest().body(Map.of("message", "Only CSV files are allowed"));
            }

            logger.info("Processing CSV file...");
            List<Hotel> savedHotels = hotelService.processCSVFile(file);
            logger.info("Successfully processed and saved {} hotels", savedHotels.size());
            
            return ResponseEntity.ok(Map.of(
                "message", "Successfully uploaded " + savedHotels.size() + " hotels",
                "hotels", savedHotels
            ));
        } catch (IOException e) {
            logger.error("Error processing CSV file: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Error processing CSV file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during CSV processing: {}", e.getMessage(), e);
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Error processing CSV file: " + e.getMessage()));
        }
    }
}
