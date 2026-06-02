package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // Fixed: cr-java-0067 - Replace in-memory caching with Amazon ElastiCache for Redis
    // Distributed cache with TTL ensures consistency across multiple instances
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Fixed: cr-java-0071 - Externalize environment URLs using AWS Systems Manager Parameter Store
    @Value("${app.inventory.endpoint}")
    private String inventoryUrl;

    private static final String CACHE_KEY_PREFIX = "booking:";
    private static final long CACHE_TTL_MINUTES = 30;

    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // Fixed: cr-java-0065 - Replace HTTP session storage with Amazon ElastiCache for Redis
        // Store booking data in Redis with TTL for distributed access across all instances
        String bookingId = (String) booking.get("bookingId");
        String cacheKey = CACHE_KEY_PREFIX + bookingId;
        redisTemplate.opsForValue().set(cacheKey, booking, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        
        // Store guest name mapping for quick lookup
        String guestCacheKey = CACHE_KEY_PREFIX + "guest:" + bookingId;
        redisTemplate.opsForValue().set(guestCacheKey, guestName, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(@PathVariable String bookingId) {

        // Fixed: cr-java-0065 - Replace HTTP session storage with Amazon ElastiCache for Redis
        // Retrieve guest name from Redis instead of HTTP session
        String guestCacheKey = CACHE_KEY_PREFIX + "guest:" + bookingId;
        String lastGuest = (String) redisTemplate.opsForValue().get(guestCacheKey);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // Fixed: cr-java-0071 - Externalize environment URLs using AWS Systems Manager Parameter Store
        // Use externalized configuration instead of hard-coded URL
        String availabilityEndpoint = inventoryUrl + "/available";

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", availabilityEndpoint);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // Fixed: cr-java-0061, cr-java-0062, cr-java-0063 - Replace hard-coded file paths with Amazon S3
        // Report generation now uses S3 storage instead of local file system
        String reportMessage = bookingService.generateReport(month);

        Map<String, Object> response = new HashMap<>();
        response.put("message", reportMessage);
        response.put("storageType", "S3");
        return response;
    }
}
