package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    // cr-java-0061 FIX: Replaced hardcoded absolute file path "/var/legacy/reports/" with
    // an environment-variable-backed S3 bucket name. The S3 bucket is resolved at runtime
    // from the AWS_REPORTS_BUCKET environment variable (or application property), eliminating
    // any dependency on the host file system.
    @Value("${aws.s3.reports.bucket:${AWS_REPORTS_BUCKET:resorts-lite-reports}}")
    private String reportsBucket;

    // cr-java-0061 FIX: Replaced hardcoded Windows-style backup path "C:\\ResortBackups\\nightly\\"
    // with an S3 key prefix resolved from an environment variable. No local file system path
    // is referenced; all backup objects are stored as S3 objects under this prefix.
    @Value("${aws.s3.backup.prefix:${AWS_BACKUP_PREFIX:backups/nightly/}}")
    private String backupPrefix;

    // cr-java-0061 FIX: Replaced hardcoded absolute path "/var/legacy/reports/" with
    // an S3 key prefix resolved from an environment variable, used when constructing
    // the S3 object key for report files.
    @Value("${aws.s3.reports.prefix:${AWS_REPORTS_PREFIX:reports/}}")
    private String reportsPrefix;

    @Value("${aws.region:${AWS_REGION:us-east-1}}")
    private String awsRegion;

    /**
     * Generates a monthly resort report and uploads it to Amazon S3.
     * Replaces the previous implementation that wrote to the local file system
     * at the hardcoded path "/var/legacy/reports/" (cr-java-0061).
     *
     * @param month the month for which the report is generated
     * @param year  the year for which the report is generated
     * @return a map containing the operation status and the S3 object key
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        String fileName = "resort_report_" + month + "_" + year + ".csv";
        // cr-java-0061 FIX (Line 37): Replaced local fullPath = REPORT_BASE_PATH + fileName
        // with an S3 object key. No local File or FileWriter is used.
        String s3ObjectKey = reportsPrefix + fileName;

        Map<String, Object> result = new HashMap<>();

        try {
            S3Client s3 = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .build();

            String csvContent = "BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n"
                    + "BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n"
                    + "BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n";

            // cr-java-0061 FIX (Line 42): Replaced new File(REPORT_BASE_PATH) / mkdirs() /
            // FileWriter with an S3 PutObjectRequest. S3 does not require directory creation;
            // the bucket and key prefix are sufficient to organise objects.
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(reportsBucket)
                    .key(s3ObjectKey)
                    .contentType("text/csv")
                    .build();

            s3.putObject(putRequest, RequestBody.fromString(csvContent));

            result.put("status", "generated");
            result.put("s3Bucket", reportsBucket);
            result.put("s3Key", s3ObjectKey);

        } catch (S3Exception e) {
            result.put("status", "error");
            result.put("message", e.awsErrorDetails().errorMessage());
        }

        return result;
    }

    /**
     * Builds a report download URL.
     *
     * @param reportName the name of the report file
     * @return the download URL string
     */
    public String buildReportDownloadUrl(String reportName) {
        // cr-java-0088 FIX: Plain HTTP URL replaced with HTTPS.
        return "https://reports.resorts-internal.com:8080/download/" + reportName;
    }

    /**
     * Returns system information including S3 storage configuration.
     * Replaces the previous implementation that exposed local file system paths
     * (cr-java-0061).
     *
     * @return a map of system information key-value pairs
     */
    public Map<String, Object> getSystemInfo() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Map<String, Object> info = new HashMap<>();
        // cr-java-0061 FIX: Replaced REPORT_BASE_PATH and BACKUP_PATH (hardcoded local paths)
        // with S3 bucket/prefix values sourced from environment variables.
        info.put("reportsBucket", reportsBucket);
        info.put("reportsPrefix", reportsPrefix);
        info.put("backupPrefix", backupPrefix);
        info.put("storageType", "Amazon S3");
        info.put("generatedAt", timestamp);
        return info;
    }
}
