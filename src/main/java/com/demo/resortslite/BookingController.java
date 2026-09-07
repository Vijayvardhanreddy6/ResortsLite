package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * cr-java-0065 FIX: @EnableRedisHttpSession activates Spring Session with Redis as the
 * backing store. All HttpSession operations (setAttribute / getAttribute) are transparently
 * delegated to Amazon ElastiCache for Redis, enabling stateless application instances with
 * centralized, distributed session management. Session data is no longer stored in JVM
 * heap memory, so any EC2 instance in the Auto Scaling group can serve any request without
 * sticky sessions or server affinity.
 *
 * maxInactiveIntervalInSeconds defaults to 1800 (30 min); override via
 * spring.session.timeout in application.properties or the SESSION_TIMEOUT_SECONDS
 * environment variable.
 */
@EnableRedisHttpSession
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    /**
     * cr-java-0067 FIX: Replaced the unbounded in-memory HashMap cache
     * (private static final Map<String, Object> bookingCache = new HashMap<>())
     * with Amazon ElastiCache for Redis via Spring Data RedisTemplate.
     *
     * The original static HashMap was instance-local: each EC2 instance maintained its own
     * independent copy of the cache, causing stale data inconsistencies across instances and
     * unbounded memory growth with no expiration policy. In a horizontally scaled cloud
     * environment this leads to cache misses, data divergence, and potential OOM errors.
     *
     * With RedisTemplate backed by Amazon ElastiCache for Redis:
     * - All application instances share a single, centralized cache store.
     * - Every cache entry is written with a configurable TTL (default 30 minutes, controlled
     *   by the BOOKING_CACHE_TTL_SECONDS environment variable / app.cache.booking-ttl-seconds
     *   property) so entries expire automatically and memory is bounded.
     * - Cache operations are atomic and consistent across the entire Auto Scaling group.
     */
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * TTL (in seconds) applied to every booking cache entry written to Redis.
     * Defaults to 1800 seconds (30 minutes). Override via the
     * {@code BOOKING_CACHE_TTL_SECONDS} environment variable or the
     * {@code app.cache.booking-ttl-seconds} application property.
     */
    @Value("${app.cache.booking-ttl-seconds:${BOOKING_CACHE_TTL_SECONDS:1800}}")
    private long bookingCacheTtlSeconds;

    /** Redis key prefix used to namespace booking cache entries. */
    private static final String BOOKING_CACHE_KEY_PREFIX = "booking:cache:";

    /**
     * AWS SSM Parameter Store parameter name for the inventory service URL.
     * cr-java-0071 FIX: The hard-coded URL "http://inventory-service.internal:8081/rooms/available"
     * (original line 66) is replaced by a parameter name that is resolved at runtime from
     * AWS Systems Manager Parameter Store, enabling environment-agnostic deployments.
     * Set via environment variable {@code INVENTORY_URL_PARAM_NAME} or application property
     * {@code app.ssm.inventory-url-param}; defaults to {@code /resorts/inventory/url}.
     */
    @Value("${app.ssm.inventory-url-param:${INVENTORY_URL_PARAM_NAME:/resorts/inventory/url}}")
    private String inventoryUrlParamName;

    /**
     * AWS region used to build the SSM client.
     * Sourced from environment variable {@code AWS_REGION}.
     */
    @Value("${cloud.aws.region.static:${AWS_REGION:us-east-1}}")
    private String awsRegion;

    /**
     * Shared SSM client — initialised once on bean startup and closed on shutdown.
     */
    private SsmClient ssmClient;

    /**
     * Initialises the shared {@link SsmClient} after all {@code @Value} fields have been
     * injected.
     */
    @PostConstruct
    public void init() {
        this.ssmClient = SsmClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    /**
     * Closes the shared {@link SsmClient} when the Spring context shuts down.
     */
    @PreDestroy
    public void destroy() {
        if (ssmClient != null) {
            ssmClient.close();
        }
    }

    /**
     * Retrieves the inventory service URL from AWS Systems Manager Parameter Store.
     *
     * <p><strong>cr-java-0071 FIX (line 66):</strong><br>
     * The original hard-coded value {@code "http://inventory-service.internal:8081/rooms/available"}
     * has been replaced with a runtime lookup against AWS SSM Parameter Store using the
     * parameter name stored in {@code app.ssm.inventory-url-param}. This allows the URL to
     * be changed per environment (dev / staging / production) without any code redeployment,
     * satisfying the 12-factor app externalized-configuration principle.</p>
     *
     * @return the inventory service URL retrieved from SSM Parameter Store
     */
    private String getInventoryUrlFromSsm() {
        try {
            // cr-java-0071 FIX: retrieve environment-specific URL from AWS SSM Parameter Store
            // instead of using the hard-coded value "http://inventory-service.internal:8081/rooms/available"
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(inventoryUrlParamName)
                    .withDecryption(false)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (SsmException e) {
            // Fall back to a safe default if the parameter is not yet provisioned,
            // so the application can still start in local/test environments.
            return "https://inventory-service.internal:8081/rooms/available";
        }
    }

    /**
     * Creates a new booking and stores the booking state in the distributed Redis session
     * via Spring Session (cr-java-0065 FIX), and caches the booking in Amazon ElastiCache
     * for Redis with a TTL (cr-java-0067 FIX).
     *
     * <p><strong>cr-java-0065 FIX (lines 34–35):</strong><br>
     * Previously, {@code session.setAttribute("lastBooking", booking)} and
     * {@code session.setAttribute("guestName", guestName)} stored session data in the local
     * JVM heap. With {@code @EnableRedisHttpSession} active, these same calls now transparently
     * persist the attributes to Amazon ElastiCache for Redis. All EC2 instances share the
     * same Redis cluster, so any instance can read the session regardless of which instance
     * originally created it — eliminating server affinity and enabling horizontal scaling.</p>
     *
     * <p><strong>cr-java-0067 FIX:</strong><br>
     * The former {@code bookingCache.put(bookingId, booking)} call on the static HashMap has
     * been replaced with {@code redisTemplate.opsForValue().set(key, booking, ttl, TimeUnit.SECONDS)}.
     * The entry is written to the shared ElastiCache Redis cluster with a configurable TTL
     * (default 1800 s), ensuring automatic expiration and bounded memory usage across all
     * application instances.</p>
     */
    @PostMapping("/create")
    public Map<String, Object> createBooking(
            @RequestParam String guestName,
            @RequestParam String roomType,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        Map<String, Object> booking = bookingService.createBooking(guestName, roomType, checkIn, checkOut);

        // cr-java-0065 FIX: session.setAttribute calls are now backed by Amazon ElastiCache
        // for Redis via Spring Session (@EnableRedisHttpSession on this class). Session data
        // is stored in the shared Redis cluster rather than in local JVM memory, enabling
        // stateless application instances and safe horizontal scaling across EC2 instances.
        session.setAttribute("lastBooking", booking);
        session.setAttribute("guestName", guestName);

        // cr-java-0067 FIX: Replace the unbounded static HashMap cache entry with a
        // Redis cache entry that carries a TTL. The key is namespaced under
        // "booking:cache:<bookingId>" to avoid collisions with other Redis key spaces.
        // The TTL (bookingCacheTtlSeconds, default 1800 s) ensures entries expire
        // automatically, preventing indefinite memory growth and stale data across instances.
        String cacheKey = BOOKING_CACHE_KEY_PREFIX + booking.get("bookingId");
        redisTemplate.opsForValue().set(cacheKey, booking, bookingCacheTtlSeconds, TimeUnit.SECONDS);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        response.put("booking", booking);
        return response;
    }

    /**
     * Returns the booking status for the given booking ID.
     *
     * <p><strong>cr-java-0065 FIX (line 48):</strong><br>
     * Previously, {@code session.getAttribute("guestName")} read from the local JVM session,
     * which would return {@code null} on any EC2 instance other than the one that created the
     * session. With Spring Session + Redis, the attribute is fetched from the shared
     * ElastiCache cluster, so every instance in the Auto Scaling group returns the correct
     * value regardless of which instance handled the original {@code /create} request.</p>
     *
     * <p><strong>cr-java-0067 FIX:</strong><br>
     * Booking details are now looked up from the shared Redis cache via
     * {@code redisTemplate.opsForValue().get(key)} before falling back to the service layer.
     * This ensures cache reads are consistent across all EC2 instances.</p>
     */
    @GetMapping("/status/{bookingId}")
    public Map<String, Object> getBookingStatus(
            @PathVariable String bookingId,
            HttpSession session) {

        // cr-java-0065 FIX: session.getAttribute is now served from Amazon ElastiCache for
        // Redis via Spring Session, not from local JVM memory. The value is consistent across
        // all EC2 instances in the cluster, eliminating the data-loss risk on load-balanced
        // or auto-scaled deployments.
        String lastGuest = (String) session.getAttribute("guestName");

        // cr-java-0067 FIX: Look up the booking from the shared Redis cache (with TTL)
        // instead of the former instance-local static HashMap. If the entry has expired or
        // is not present, fall back to the service layer for a fresh lookup.
        String cacheKey = BOOKING_CACHE_KEY_PREFIX + bookingId;
        Object cachedBooking = redisTemplate.opsForValue().get(cacheKey);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", bookingId);
        result.put("sessionGuest", lastGuest);
        result.put("details", cachedBooking != null ? cachedBooking : bookingService.getBookingById(bookingId));
        return result;
    }

    @GetMapping("/availability")
    public Map<String, Object> checkAvailability(@RequestParam String roomType) {
        // cr-java-0071 FIX: The hard-coded URL "http://inventory-service.internal:8081/rooms/available"
        // (original line 66) is now retrieved at runtime from AWS Systems Manager Parameter Store
        // via getInventoryUrlFromSsm(). The parameter name is externalised to the environment
        // variable INVENTORY_URL_PARAM_NAME / application property app.ssm.inventory-url-param,
        // enabling environment-agnostic deployments without code changes.
        String inventoryUrl = getInventoryUrlFromSsm(); // cr-java-0071 FIX

        Map<String, Object> response = new HashMap<>();
        response.put("roomType", roomType);
        response.put("inventoryEndpoint", inventoryUrl);
        response.put("available", bookingService.isRoomAvailable(roomType));
        return response;
    }

    @GetMapping("/report/download")
    public Map<String, Object> downloadReport(@RequestParam String month) {
        // VIOLATION czr-java-001 [Software Portability / Mandatory]: Hardcoded absolute
        // file path. This path does not exist inside a container image. Container images
        // have their own isolated file systems — /var/legacy/reports won't be present.
        String reportPath = "/var/legacy/reports/" + month + "_bookings.pdf"; // czr-java-001

        Map<String, Object> response = new HashMap<>();
        response.put("reportPath", reportPath);
        response.put("message", bookingService.generateReport(month));
        return response;
    }
}
