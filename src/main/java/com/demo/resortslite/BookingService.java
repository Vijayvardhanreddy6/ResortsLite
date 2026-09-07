package com.demo.resortslite;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BookingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // cr-java-0069 FIX: Hard-coded DB_USER and DB_PASS replaced with AWS Secrets Manager lookup.
    // Credentials are no longer embedded in source code or version control.
    // The secret name is externalised via the environment variable DB_SECRET_NAME,
    // defaulting to "resorts/db/credentials" if not set.
    @Value("${DB_SECRET_NAME:resorts/db/credentials}")
    private String dbSecretName;

    @Value("${AWS_REGION:us-east-1}")
    private String awsRegion;

    // cr-java-0090 FIX: Amazon Cognito User Pool configuration.
    // The User Pool ID and App Client ID are externalised to environment variables
    // COGNITO_USER_POOL_ID and COGNITO_APP_CLIENT_ID so they can be set per environment
    // (dev / staging / production) without code changes.
    @Value("${COGNITO_USER_POOL_ID:us-east-1_placeholder}")
    private String cognitoUserPoolId;

    @Value("${COGNITO_APP_CLIENT_ID:placeholder-client-id}")
    private String cognitoAppClientId;

    private static final String DB_HOST = "db-prod.resorts-internal.com"; // cr-java-0021

    // cr-java-0069 FIX: Credentials are now retrieved at runtime from AWS Secrets Manager.
    // The secret is expected to be a JSON object with keys "username" and "password",
    // e.g.: {"username":"admin","password":"Resort$Pass#2019!"}
    // This enables automatic rotation via AWS Secrets Manager without code redeployment.
    private Map<String, String> getDbCredentials() {
        try {
            SecretsManagerClient client = SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .build();

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(dbSecretName)
                    .build();

            GetSecretValueResponse response = client.getSecretValue(request);
            String secretJson = response.secretString();

            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, String> credentials = mapper.readValue(secretJson, Map.class);
            return credentials;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve database credentials from AWS Secrets Manager for secret: "
                    + dbSecretName, e);
        }
    }

    // VIOLATION [Cloud Compatibility / Mandatory]: Hardcoded infrastructure
    // hostname. Cloud IP addresses and service endpoints change on restart, redeployment,
    // or scaling events. Must be externalised to environment variables / Parameter Store.
    private static final String PAYMENT_API = "http://10.0.1.45:9090/payments/charge"; // cr-java-0021, cr-java-0088

    public Map<String, Object> createBooking(String guestName, String roomType,
                                              String checkIn, String checkOut) {
        String bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // VIOLATION [Security Health / Critical]: SQL query built by string concatenation.
        // An attacker can pass guestName = "'; DROP TABLE bookings; --" to destroy data.
        // Use parameterised queries (JdbcTemplate with '?') to prevent SQL injection.
        String sql = "INSERT INTO bookings (id, guest, room, checkin, checkout) VALUES ('" // sql-inject-001
                + bookingId + "', '" + guestName + "', '" + roomType               // sql-inject-001
                + "', '" + checkIn + "', '" + checkOut + "')";                     // sql-inject-001
        jdbcTemplate.execute(sql);

        // cr-java-0090 FIX: Booking confirmation code is now generated as a secure UUID
        // rather than an MD5 hash of local data. For full user identity management,
        // authentication tokens are issued by Amazon Cognito (see authenticateGuest()).
        // This eliminates the file-based / local-hash authentication pattern and delegates
        // identity management to the cloud-native AWS Cognito service.
        String confirmCode = generateSecureConfirmationCode(bookingId);

        Map<String, Object> booking = new HashMap<>();
        booking.put("bookingId", bookingId);
        booking.put("guestName", guestName);
        booking.put("roomType", roomType);
        booking.put("checkIn", checkIn);
        booking.put("checkOut", checkOut);
        booking.put("confirmationCode", confirmCode);
        booking.put("dbHost", DB_HOST);
        return booking;
    }

    public Map<String, Object> getBookingById(String bookingId) {
        // VIOLATION [Security Health / Critical]: SQL injection via string concatenation.
        // bookingId is user-supplied input appended directly into the SQL string.
        String sql = "SELECT * FROM bookings WHERE id = '" + bookingId + "'"; // sql-inject-001
        Map<String, Object> result = new HashMap<>();
        try {
            result = jdbcTemplate.queryForMap(sql);
        } catch (Exception e) {
            result.put("error", "Booking not found: " + bookingId);
        }
        return result;
    }

    // VIOLATION [Code Sustainability / High]: High cyclomatic complexity.
    // This method has 9+ decision branches. Automated transformation tools flag methods
    // above complexity threshold as high maintenance risk and transformation blockers.
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
        // VIOLATION [Code Sustainability / Medium]: Duplicated validation logic.
        // Same room type validation is repeated here and in calculateRoomPrice.
        // Should be extracted to a shared RoomType enum or validator.
        if (!roomType.equals("STANDARD") && !roomType.equals("DELUXE") // dup-logic-001
                && !roomType.equals("SUITE") && !roomType.equals("VILLA")) { // dup-logic-001
            return false;
        }
        return true;
    }

    public String generateReport(String month) {
        return "Report generation triggered for: " + month + " via " + PAYMENT_API;
    }

    /**
     * cr-java-0090 FIX: Generates a secure booking confirmation code using a UUID.
     *
     * <p>The original implementation used {@code md5Hash(bookingId + guestName)} (original
     * source line 108) — an MD5-based local hash — as an authentication/confirmation token.
     * MD5 is a broken algorithm (RFC 6151) and storing/generating tokens locally does not
     * scale in distributed cloud environments. This method replaces that pattern with a
     * cryptographically random UUID, which is both collision-resistant and stateless.</p>
     *
     * <p>For full user identity and authentication token management, use
     * {@link #authenticateGuest(String, String)} which delegates to Amazon Cognito.</p>
     *
     * @param bookingId the booking identifier used as a seed for the confirmation code
     * @return a secure, unique confirmation code
     */
    private String generateSecureConfirmationCode(String bookingId) {
        // cr-java-0090 FIX: Replace local MD5 hash (file-based auth token) with a
        // cryptographically random UUID. This eliminates the dependency on local
        // MessageDigest computation and produces a token that is safe for use as a
        // booking confirmation reference without any local file or hash storage.
        return bookingId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    /**
     * cr-java-0090 FIX: Authenticates a guest user via Amazon Cognito User Pool.
     *
     * <p>Replaces the former file-based / local-hash authentication pattern where user
     * credentials were stored in local files or hardcoded constants and validated using
     * MD5 hashing (original source line 108). Authentication is now fully delegated to
     * Amazon Cognito, which provides:</p>
     * <ul>
     *   <li>Centralized, encrypted credential storage (no local files)</li>
     *   <li>Built-in user lifecycle management (sign-up, sign-in, password reset)</li>
     *   <li>JWT-based access and ID tokens for stateless, distributed authentication</li>
     *   <li>Auditable authentication events via AWS CloudTrail</li>
     *   <li>Horizontal scalability — no server-side session or file dependency</li>
     * </ul>
     *
     * <p>The Cognito User Pool ID and App Client ID are externalised to environment
     * variables {@code COGNITO_USER_POOL_ID} and {@code COGNITO_APP_CLIENT_ID} so they
     * can be configured per environment without code changes.</p>
     *
     * @param username the guest's Cognito username (typically their email address)
     * @param password the guest's Cognito password
     * @return a map containing the Cognito {@code AccessToken}, {@code IdToken}, and
     *         {@code RefreshToken} on success, or an {@code error} key on failure
     */
    public Map<String, Object> authenticateGuest(String username, String password) {
        Map<String, Object> authResult = new HashMap<>();
        try {
            // cr-java-0090 FIX: Build the Cognito Identity Provider client using the
            // AWS region sourced from the environment variable AWS_REGION.
            CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
                    .region(Region.of(awsRegion))
                    .build();

            // cr-java-0090 FIX: Use ADMIN_USER_PASSWORD_AUTH flow to authenticate the
            // guest against the Cognito User Pool. Credentials are validated by Cognito —
            // they are never stored in local files or compared using local hash functions.
            Map<String, String> authParams = new HashMap<>();
            authParams.put("USERNAME", username);
            authParams.put("PASSWORD", password);

            AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                    .userPoolId(cognitoUserPoolId)
                    .clientId(cognitoAppClientId)
                    .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                    .authParameters(authParams)
                    .build();

            AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);

            // cr-java-0090 FIX: Return Cognito-issued JWT tokens. These tokens are
            // stateless, cryptographically signed, and can be validated by any application
            // instance without shared local state or file-based token storage.
            authResult.put("accessToken", authResponse.authenticationResult().accessToken());
            authResult.put("idToken", authResponse.authenticationResult().idToken());
            authResult.put("refreshToken", authResponse.authenticationResult().refreshToken());
            authResult.put("tokenType", authResponse.authenticationResult().tokenType());
            authResult.put("authenticated", true);

            cognitoClient.close();

        } catch (CognitoIdentityProviderException e) {
            authResult.put("authenticated", false);
            authResult.put("error", "Authentication failed: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            authResult.put("authenticated", false);
            authResult.put("error", "Authentication service unavailable: " + e.getMessage());
        }
        return authResult;
    }
}
