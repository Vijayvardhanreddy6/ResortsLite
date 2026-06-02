package com.demo.resortslite;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecretsManagerClient secretsManagerClient;

    // Fixed: cr-java-0069 - Replace hard-coded database credentials with AWS Secrets Manager
    // Credentials are now retrieved from AWS Secrets Manager at runtime
    @Value("${aws.secrets.db.secret.name}")
    private String dbSecretName;

    // Fixed: cr-java-0071 - Externalize environment URLs using AWS Systems Manager Parameter Store
    @Value("${app.payment.endpoint}")
    private String paymentApiEndpoint;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retrieves database credentials from AWS Secrets Manager
     * Fixed: cr-java-0069 - Centralized, encrypted secret storage with automatic rotation support
     */
    private Map<String, String> getDatabaseCredentials() {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbSecretName)
                    .build();
            
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            String secretString = response.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            Map<String, String> credentials = new HashMap<>();
            credentials.put("host", secretJson.get("host").asText());
            credentials.put("username", secretJson.get("username").asText());
            credentials.put("password", secretJson.get("password").asText());
            
            return credentials;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve database credentials from Secrets Manager", e);
        }
    }

    /**
     * Fixed: cr-java-0090 - Replace file-based authentication with AWS Secrets Manager
     * Authentication credentials are now stored in AWS Secrets Manager instead of local files
     */
    private String getAuthenticationToken(String userId) {
        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId("resortslite/auth/tokens/" + userId)
                    .build();
            
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);
            return response.secretString();
        } catch (Exception e) {
            // Return null if token not found, allowing application to handle authentication flow
            return null;
        }
    }

    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Fixed: SQL injection vulnerability - Use parameterized queries
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, bookingId, guestName, roomType, checkIn, checkOut);

        // Fixed: Security vulnerability - Use SHA-256 instead of MD5
        String confirmCode = sha256Hash(bookingId + guestName);

        // Fixed: cr-java-0069 - Retrieve DB host from Secrets Manager instead of hard-coded value
        Map<String, String> dbCredentials = getDatabaseCredentials();

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        booking.put("dbHost", dbCredentials.get("host"));
        return booking;
    }

    public Map<String, Object> getBookingById(String bookingId) {
        // Fixed: SQL injection vulnerability - Use parameterized queries
        String sql = "SELECT * FROM bookings WHERE id = ?";
        Map<String, Object> result = new HashMap<>();
        try {
            result = jdbcTemplate.queryForMap(sql, bookingId);
        } catch (Exception e) {
            result.put("error", "Booking not found: " + bookingId);
        }
        return result;
    }

    public String calculateRoomPrice(String roomType, int nights, String season, String loyalty) {
        double basePrice = 0;
        if (roomType.equals("STANDARD")) { basePrice = 120.0; }
        else if (roomType.equals("DELUXE")) { basePrice = 200.0; }
        else if (roomType.equals("SUITE")) { basePrice = 350.0; }
        else if (roomType.equals("VILLA")) { basePrice = 600.0; }
        else { basePrice = 120.0; }
        if (season.equals("PEAK")) { basePrice = basePrice * 1.5; }
        else if (season.equals("OFF")) { basePrice = basePrice * 0.8; }
        if (loyalty.equals("GOLD")) { basePrice = basePrice * 0.9; }
        else if (loyalty.equals("PLATINUM")) { basePrice = basePrice * 0.8; }
        else if (loyalty.equals("DIAMOND")) { basePrice = basePrice * 0.7; }
        if (nights >= 7) { basePrice = basePrice * 0.95; }
        else if (nights >= 14) { basePrice = basePrice * 0.90; }
        double total = basePrice * nights;
        return String.format("%.2f", total);
    }

    public boolean isRoomAvailable(String roomType) {
        if (!roomType.equals("STANDARD") && !roomType.equals("DELUXE")
                && !roomType.equals("SUITE") && !roomType.equals("VILLA")) {
            return false;
        }
        return true;
    }

    public String generateReport(String month) {
        // Fixed: cr-java-0071 - Use externalized payment endpoint from configuration
        return "Report generation triggered for: " + month + " via " + paymentApiEndpoint;
    }

    /**
     * Fixed: Security vulnerability - Use SHA-256 instead of MD5 for hashing
     */
    private String sha256Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) { sb.append(String.format("%02x", b)); }
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
