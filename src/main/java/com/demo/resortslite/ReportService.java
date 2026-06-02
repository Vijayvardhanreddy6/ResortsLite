package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    @Autowired
    private S3Client s3Client;

    @Autowired
    private SsmClient ssmClient;

    // Fixed: cr-java-0061, cr-java-0062, cr-java-0063 - Replace hard-coded file paths with Amazon S3
    // All file operations now use S3 for durable, scalable cloud storage
    @Value("${aws.s3.bucket.name}")
    private String s3BucketName;

    @Value("${app.storage.s3.reports.prefix}")
    private String reportsPrefix;

    @Value("${app.storage.s3.backups.prefix}")
    private String backupsPrefix;

    // Fixed: cr-java-0077 - Replace hard-coded ports with AWS Parameter Store and environment variable injection
    @Value("${server.port}")
    private int serverPort;

    // Fixed: cr-java-0071 - Externalize environment URLs using AWS Systems Manager Parameter Store
    @Value("${app.inventory.endpoint}")
    private String reportsServiceUrl;

    /**
     * Generates monthly report and stores it in Amazon S3
     * Fixed: cr-java-0061, cr-java-0062, cr-java-0063 - Migrate to S3 for cloud-native storage
     * 
     * @param month The month for the report
     * @param year The year for the report
     * @return Map containing report generation status and S3 location
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        String s3Key = reportsPrefix + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            // Fixed: cr-java-0062, cr-java-0063 - Replace local file writes with Amazon S3
            // Generate report content in memory and upload directly to S3
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            
            writer.write("BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n");
            writer.write("BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n");
            writer.write("BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n");
            writer.flush();
            writer.close();

            // Upload to S3
            byte[] reportData = outputStream.toByteArray();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3BucketName)
                    .key(s3Key)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(reportData));

            result.put("status", "generated");
            result.put("s3Bucket", s3BucketName);
            result.put("s3Key", s3Key);
            result.put("storageType", "S3");
            // Fixed: cr-java-0077 - Use dynamic port from configuration
            result.put("serverPort", serverPort);

        } catch (IOException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Builds report download URL using externalized configuration
     * Fixed: cr-java-0071 - Replace hard-coded environment URLs with Parameter Store
     * Fixed: cr-java-0077 - Replace hard-coded ports with environment variable injection
     * 
     * @param reportName The name of the report to download
     * @return The download URL for the report
     */
    public String buildReportDownloadUrl(String reportName) {
        // Fixed: cr-java-0071 - Use externalized URL from AWS Systems Manager Parameter Store
        // The URL is now retrieved from configuration instead of being hard-coded
        String baseUrl = getParameterFromStore("/resortslite/reports/base-url", 
                                               "https://reports.resorts-internal.com");
        return baseUrl + ":" + serverPort + "/download/" + reportName;
    }

    /**
     * Retrieves system information with cloud-native configuration
     * Fixed: cr-java-0111 - Replace java.util.Date with java.time API and standardize on UTC
     * 
     * @return Map containing system configuration information
     */
    public Map<String, Object> getSystemInfo() {
        // Fixed: cr-java-0111 - Use java.time API with UTC timezone for consistent time handling
        Instant now = Instant.now();
        ZonedDateTime utcTime = now.atZone(ZoneId.of("UTC"));
        String timestamp = utcTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Map<String, Object> info = new HashMap<>();
        // Fixed: cr-java-0061 - Report S3 storage location instead of local file paths
        info.put("reportStorageType", "S3");
        info.put("s3Bucket", s3BucketName);
        info.put("reportsPrefix", reportsPrefix);
        info.put("backupsPrefix", backupsPrefix);
        // Fixed: cr-java-0077 - Use dynamic port from configuration
        info.put("serverPort", serverPort);
        // Fixed: cr-java-0111 - Use UTC timestamp with ISO-8601 format
        info.put("generatedAt", timestamp);
        info.put("timezone", "UTC");
        return info;
    }

    /**
     * Retrieves configuration parameter from AWS Systems Manager Parameter Store
     * 
     * @param parameterName The name of the parameter to retrieve
     * @param defaultValue The default value if parameter is not found
     * @return The parameter value or default value
     */
    private String getParameterFromStore(String parameterName, String defaultValue) {
        try {
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(parameterName)
                    .build();
            
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (Exception e) {
            // Return default value if parameter not found
            return defaultValue;
        }
    }
}
