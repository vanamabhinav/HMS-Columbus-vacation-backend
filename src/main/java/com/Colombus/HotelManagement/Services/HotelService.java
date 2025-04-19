package com.Colombus.HotelManagement.Services;

import com.Colombus.HotelManagement.Models.Hotel;
import com.Colombus.HotelManagement.Repositories.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class HotelService {
    private static final Logger logger = LoggerFactory.getLogger(HotelService.class);

    @Autowired
    private HotelRepository hotelRepository;

    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    public Optional<Hotel> getHotelById(Long id) {
        return hotelRepository.findById(id);
    }

    public Hotel saveHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    public void deleteHotel(Long id) {
        hotelRepository.deleteById(id);
    }

    public List<Hotel> searchHotelsByName(String name) {
        return hotelRepository.findByHotelNameContainingIgnoreCase(name);
    }

    public List<Hotel> getPreferredHotels() {
        return hotelRepository.findByPreferredTrue();
    }

    public List<Hotel> searchHotelsByCity(String city) {
        return hotelRepository.findByCityContainingIgnoreCase(city);
    }

    public List<Hotel> searchHotelsByState(String state) {
        return hotelRepository.findByStateContainingIgnoreCase(state);
    }

    public List<Hotel> searchHotelsByCityAndState(String city, String state) {
        return hotelRepository.findByCityContainingIgnoreCaseAndStateContainingIgnoreCase(city, state);
    }

    public Hotel addHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    // Update hotel details
    public Hotel updateHotel(Long id, Hotel updatedHotel) {
        return hotelRepository.findById(id).map(hotel -> {
            hotel.setHotelName(updatedHotel.getHotelName());
            hotel.setEmail1(updatedHotel.getEmail1());
            hotel.setEmail2(updatedHotel.getEmail2());
            hotel.setAddress(updatedHotel.getAddress());
            hotel.setMobilePhoneContact(updatedHotel.getMobilePhoneContact());
            hotel.setLandlineContact(updatedHotel.getLandlineContact());
            hotel.setConcerningPersonName(updatedHotel.getConcerningPersonName());
            hotel.setPreferred(updatedHotel.isPreferred());
            hotel.setCity(updatedHotel.getCity());
            hotel.setState(updatedHotel.getState());
            hotel.setWebsite(updatedHotel.getWebsite());
            return hotelRepository.save(hotel);
        }).orElseThrow(() -> new RuntimeException("Hotel not found"));
    }

    public List<Hotel> processCSVFile(MultipartFile file) throws IOException {
        List<Hotel> hotels = new ArrayList<>();
        List<Hotel> savedHotels = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int lineNumber = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            // Skip header line
            reader.readLine();
            lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                logger.info("Processing line {}: {}", lineNumber, line);
                
                try {
                    String[] data = line.split(",");
                    if (data.length < 11) { // Ensure we have all required fields
                        String error = "Line " + lineNumber + " has insufficient fields: " + data.length + " (needs at least 11)";
                        logger.warn(error);
                        errors.add(error);
                        continue;
                    }
                    
                    Hotel hotel = new Hotel();
                    hotel.setHotelName(data[0].trim());
                    hotel.setEmail1(data[1].trim());
                    hotel.setEmail2(data[2].trim().isEmpty() ? null : data[2].trim());
                    hotel.setAddress(data[3].trim());
                    hotel.setMobilePhoneContact(data[4].trim());
                    hotel.setLandlineContact(data[5].trim().isEmpty() ? null : data[5].trim());
                    hotel.setConcerningPersonName(data[6].trim());
                    
                    try {
                        hotel.setPreferred(Boolean.parseBoolean(data[7].trim()));
                    } catch (Exception e) {
                        logger.warn("Invalid boolean value for 'preferred' at line {}: '{}'. Setting to false.", 
                            lineNumber, data[7]);
                        hotel.setPreferred(false);
                    }
                    
                    hotel.setCity(data[8].trim());
                    hotel.setState(data[9].trim());
                    hotel.setWebsite(data[10].trim().isEmpty() ? null : data[10].trim());
                    
                    // Validate required fields
                    if (isValidHotel(hotel)) {
                        logger.info("Valid hotel found at line {}: {}", lineNumber, hotel.getHotelName());
                        hotels.add(hotel);
                    } else {
                        String error = "Line " + lineNumber + " has invalid hotel data: Missing required fields";
                        logger.warn(error);
                        errors.add(error);
                    }
                } catch (Exception e) {
                    String error = "Error processing line " + lineNumber + ": " + e.getMessage();
                    logger.error(error, e);
                    errors.add(error);
                }
            }
        }
        
        if (hotels.isEmpty()) {
            logger.warn("No valid hotels found in CSV file. Errors: {}", errors);
            throw new IOException("No valid hotels found in the CSV file. Errors: " + String.join("; ", errors));
        }
        
        logger.info("Saving {} valid hotels to database", hotels.size());
        
        // Save hotels individually to handle duplicates gracefully
        for (Hotel hotel : hotels) {
            try {
                Hotel savedHotel = hotelRepository.save(hotel);
                savedHotels.add(savedHotel);
                logger.info("Successfully saved hotel: {}", hotel.getHotelName());
            } catch (Exception e) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && (
                    errorMsg.contains("Duplicate") || 
                    errorMsg.contains("constraint") || 
                    errorMsg.contains("unique"))) {
                    logger.warn("Skipping duplicate hotel: {} - {}", hotel.getHotelName(), errorMsg);
                    errors.add("Skipped duplicate: " + hotel.getHotelName() + " - " + getConstraintField(errorMsg));
                } else {
                    logger.error("Error saving hotel: " + hotel.getHotelName(), e);
                    errors.add("Error saving: " + hotel.getHotelName() + " - " + e.getMessage());
                }
            }
        }
        
        if (savedHotels.isEmpty()) {
            throw new IOException("Could not save any hotels. " + String.join("; ", errors));
        }
        
        logger.info("Successfully saved {}/{} hotels", savedHotels.size(), hotels.size());
        if (!errors.isEmpty()) {
            logger.warn("Some hotels were not saved: {}", errors);
        }
        
        return savedHotels;
    }
    
    private String getConstraintField(String errorMsg) {
        if (errorMsg.contains("email1")) return "Duplicate email1";
        if (errorMsg.contains("email2")) return "Duplicate email2";
        if (errorMsg.contains("hotel_name")) return "Duplicate hotel name";
        if (errorMsg.contains("mobile_phone_contact")) return "Duplicate mobile phone";
        if (errorMsg.contains("landline_contact")) return "Duplicate landline";
        return "Duplicate entry";
    }
    
    private boolean isValidHotel(Hotel hotel) {
        boolean isValid = hotel.getHotelName() != null && !hotel.getHotelName().trim().isEmpty() &&
                hotel.getEmail1() != null && !hotel.getEmail1().trim().isEmpty() &&
                hotel.getMobilePhoneContact() != null && !hotel.getMobilePhoneContact().trim().isEmpty() &&
                hotel.getAddress() != null && !hotel.getAddress().trim().isEmpty() &&
                hotel.getConcerningPersonName() != null && !hotel.getConcerningPersonName().trim().isEmpty() &&
                hotel.getCity() != null && !hotel.getCity().trim().isEmpty() &&
                hotel.getState() != null && !hotel.getState().trim().isEmpty();
                
        if (!isValid) {
            logger.warn("Invalid hotel data: hotelName={}, email1={}, mobilePhone={}, address={}, concerningPerson={}, city={}, state={}",
                hotel.getHotelName(),
                hotel.getEmail1(),
                hotel.getMobilePhoneContact(),
                hotel.getAddress(),
                hotel.getConcerningPersonName(),
                hotel.getCity(),
                hotel.getState()
            );
        }
        
        return isValid;
    }
}
