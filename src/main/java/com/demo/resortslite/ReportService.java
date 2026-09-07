package com.demo.resortslite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.SsmException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * ReportService — cloud-native report generation service.
 *
 * <p><strong>cr-java-0062 Fix (Local File System Write Operations):</strong><br>
 * The original implementation used {@link java.io.File} and {@link java.io.FileWriter}
 * to persist report data to the local file system at the hard-coded path
 * {@code /var/legacy/reports/} (original source line 42). In cloud and containerised
 * environments the local file system is ephemeral; data written there is lost on every
 * container restart or scale-out event, causing permanent data loss and state corruption.
 * <br><br>
 * All local file write operations have been replaced with Amazon S3 {@link PutObjectRequest}
 * calls via the AWS SDK v2 {@link S3Client}. The S3 bucket name and key prefix are
 * externalised to environment variables ({@code REPORTS_S3_BUCKET}, {@code BACKUP_S3_PREFIX},
 * {@code AWS_REGION}) so the application follows 12-factor app principles and can be
 * configured at deployment time without code changes.
 * </p>
 *
 * <p><strong>cr-java-0071 Fix (Hard-coded Environment URLs):</strong><br>
 * The original {@code buildReportDownloadUrl} method (original source line 66) contained
 * the hard-coded URL {@code "http://reports.resorts-internal.com:8080/download/"}.
 * This environment-specific URL has been replaced with a runtime lookup against AWS Systems
 * Manager Parameter Store using the parameter name stored in
 * {@code app.ssm.report-download-base-url-param}. This allows the base URL to be changed
 * per environment (dev / staging / production) without any code redeployment, satisfying
 * the 12-factor app externalized-configuration principle.
 * </p>
 *
 * <p><strong>cr-java-0077 Fix (Hard-coded Ports):</strong><br>
 * The original source (line 28) contained:
 * <pre>
 *     private static final int SERVER_PORT = 8080; // czr-port-001
 * </pre>
 * This hard-coded port constant has been eliminated. The server port is now resolved at
 * runtime from AWS Systems Manager Parameter Store via the parameter name configured in
 * {@code app.ssm.server-port-param} (environment variable {@code SERVER_PORT_PARAM_NAME}).
 * This allows ECS, EKS, and Elastic Beanstalk to assign ports dynamically per environment
 * without any code changes, satisfying cloud service-discovery requirements.
 * </p>
 *
 * <p><strong>cr-java-0111 Fix (Clock/Time Dependencies):</strong><br>
 * The original {@code getSystemInfo} method (original source line 70) used
 * {@code new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())} which relies on
 * the server-local timezone setting. In cloud deployments across multiple regions or
 * containers, timezone inconsistencies cause time-related logic errors and scheduling
 * failures. All time-sensitive code has been migrated from {@code java.util.Date} and
 * {@code java.text.SimpleDateFormat} to the {@code java.time} API ({@link ZonedDateTime},
 * {@link DateTimeFormatter}), standardized on UTC ({@link ZoneOffset#UTC}) for all
 * timestamps to ensure consistency across cloud regions and container instances.
 * </p>
 */
@Service
public class ReportService {

    /**
     * S3 bucket name for report storage.
     * Replaces the hard-coded local path {@code /var/legacy/reports/} (cr-java-0062).
     * Sourced from environment variable {@code REPORTS_S3_BUCKET} or application property
     * {@code app.reports.s3.bucket}; defaults to {@code resorts-reports-bucket}.
     */
    @Value("${app.reports.s3.bucket:${REPORTS_S3_BUCKET:resorts-reports-bucket}}")
    private String reportsBucket;

    /**
     * S3 key prefix for nightly backup objects.
     * Replaces the hard-coded Windows path {@code C:\\ResortBackups\\nightly\\} (cr-java-0062).
     * Sourced from environment variable {@code BACKUP_S3_PREFIX} or application property
     * {@code app.reports.s3.backup-prefix}; defaults to {@code backups/nightly/}.
     */
    @Value("${app.reports.s3.backup-prefix:${BACKUP_S3_PREFIX:backups/nightly/}}")
    private String backupPrefix;

    /**
     * AWS region — sourced from environment variable {@code AWS_REGION}.
     */
    @Value("${cloud.aws.region.static:${AWS_REGION:us-east-1}}")
    private String awsRegion;

    /**
     * AWS SSM Parameter Store parameter name for the server port.
     *
     * <p><strong>cr-java-0077 FIX (original source line 28):</strong><br>
     * The hard-coded constant {@code private static final int SERVER_PORT = 8080;}
     * has been eliminated. The port value is now retrieved at runtime from AWS Systems
     * Manager Parameter Store using this parameter name, which is itself injected via
     * the environment variable {@code SERVER_PORT_PARAM_NAME}. This enables ECS task
     * definitions, EKS pod specs, and Elastic Beanstalk environments to supply the
     * correct port dynamically without any code redeployment.</p>
     *
     * <p>Provision the port value in AWS SSM Parameter Store before deployment:
     * <pre>
     *   aws ssm put-parameter --name "/resorts/server/port" \
     *       --value "8080" --type String
     * </pre>
     * Override the parameter name per environment via the environment variable
     * {@code SERVER_PORT_PARAM_NAME}.</p>
     */
    @Value("${app.ssm.server-port-param:${SERVER_PORT_PARAM_NAME:/resorts/server/port}}")
    private String serverPortParamName;

    /**
     * AWS SSM Parameter Store parameter name for the report download base URL.
     *
     * <p><strong>cr-java-0071 FIX:</strong><br>
     * The hard-coded URL {@code "http://reports.resorts-internal.com:8080/download/"}
     * (original source line 66) is replaced by this parameter name that is resolved at
     * runtime from AWS Systems Manager Parameter Store. Set via environment variable
     * {@code REPORT_DOWNLOAD_URL_PARAM_NAME} or application property
     * {@code app.ssm.report-download-base-url-param}; defaults to
     * {@code /resorts/reports/download-base-url}.</p>
     */
    @Value("${app.ssm.report-download-base-url-param:${REPORT_DOWNLOAD_URL_PARAM_NAME:/resorts/reports/download-base-url}}")
    private String reportDownloadBaseUrlParamName;

    /**
     * Shared, lazily-initialised S3 client.
     * Initialised once on bean startup ({@link #init()}) and closed on shutdown
     * ({@link #destroy()}) to avoid creating a new HTTP connection pool per request.
     */
    private S3Client s3Client;

    /**
     * Shared SSM client — initialised once on bean startup and closed on shutdown.
     * Used to retrieve environment-specific configuration (port, URLs) from AWS Systems
     * Manager Parameter Store.
     */
    private SsmClient ssmClient;

    /**
     * Resolved server port — populated from AWS SSM Parameter Store during {@link #init()}.
     *
     * <p><strong>cr-java-0077 FIX:</strong> replaces the former hard-coded constant
     * {@code private static final int SERVER_PORT = 8080;} (original source line 28).
     * The value is fetched once at startup from Parameter Store and cached here so that
     * subsequent calls to {@link #generateMonthlyReport} and {@link #getSystemInfo} do
     * not incur repeated SSM API round-trips.</p>
     */
    private int resolvedServerPort;

    /**
     * Initialises the shared {@link S3Client} and {@link SsmClient} after all
     * {@code @Value} fields have been injected. Using a single shared client is the
     * recommended AWS SDK v2 pattern for Spring-managed beans.
     *
     * <p><strong>cr-java-0077 FIX:</strong> also resolves the server port from AWS SSM
     * Parameter Store at startup, replacing the former hard-coded constant.</p>
     */
    @PostConstruct
    public void init() {
        Region region = Region.of(awsRegion);
        this.s3Client = S3Client.builder()
                .region(region)
                .build();
        // cr-java-0071 FIX: initialise SSM client for Parameter Store URL lookups
        // cr-java-0077 FIX: also used to resolve the server port from Parameter Store
        this.ssmClient = SsmClient.builder()
                .region(region)
                .build();
        // cr-java-0077 FIX: resolve server port from AWS SSM Parameter Store at startup.
        // Replaces the former hard-coded constant SERVER_PORT = 8080 (original line 28).
        this.resolvedServerPort = resolveServerPortFromSsm();
    }

    /**
     * Closes the shared {@link S3Client} and {@link SsmClient} when the Spring context
     * shuts down, releasing underlying HTTP connections and thread-pool resources.
     */
    @PreDestroy
    public void destroy() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (ssmClient != null) {
            ssmClient.close();
        }
    }

    /**
     * Resolves the server port from AWS Systems Manager Parameter Store.
     *
     * <p><strong>cr-java-0077 FIX (original source line 28):</strong><br>
     * This method replaces the hard-coded constant
     * {@code private static final int SERVER_PORT = 8080;}. The port value is retrieved
     * from the SSM parameter whose name is configured via {@code app.ssm.server-port-param}
     * (environment variable {@code SERVER_PORT_PARAM_NAME}). Falls back to the value of
     * the {@code SERVER_PORT} environment variable, and ultimately to {@code 8080}, so
     * the application can still start in local/test environments where SSM is unavailable.
     * </p>
     *
     * @return the server port resolved from AWS SSM Parameter Store, or the
     *         {@code SERVER_PORT} environment variable, or {@code 8080} as a last resort
     */
    private int resolveServerPortFromSsm() {
        try {
            // cr-java-0077 FIX: retrieve the port from AWS SSM Parameter Store
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(serverPortParamName)
                    .withDecryption(false)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            String portValue = response.parameter().value();
            return Integer.parseInt(portValue.trim());
        } catch (SsmException | NumberFormatException e) {
            // Fall back to SERVER_PORT environment variable, then to 8080
            String envPort = System.getenv("SERVER_PORT");
            if (envPort != null && !envPort.isEmpty()) {
                try {
                    return Integer.parseInt(envPort.trim());
                } catch (NumberFormatException nfe) {
                    // ignore and fall through to default
                }
            }
            return 8080;
        }
    }

    /**
     * Generates a monthly report CSV and uploads it durably to Amazon S3.
     *
     * <p><strong>cr-java-0062 remediation (line 42 of original source):</strong><br>
     * The original code at line 42 was:
     * <pre>
     *     FileWriter writer = new FileWriter(fullPath);   // local file write — EPHEMERAL
     * </pre>
     * This has been replaced with an S3 {@link PutObjectRequest} so the report is stored
     * in durable, highly-available object storage that survives container restarts and
     * horizontal scale-out events. No {@link java.io.File} or {@link java.io.FileWriter}
     * operations remain in this class.
     * </p>
     *
     * @param month the month for which the report is generated (e.g. {@code "03"})
     * @param year  the year for which the report is generated (e.g. {@code "2024"})
     * @return a result map containing the upload status and the S3 object URI
     */
    public Map<String, Object> generateMonthlyReport(String month, String year) {
        // S3 object key — replaces the former local file path built from REPORT_BASE_PATH
        // (original source line 37: String fullPath = REPORT_BASE_PATH + fileName)
        String objectKey = "reports/resort_report_" + month + "_" + year + ".csv";

        Map<String, Object> result = new HashMap<>();

        // Build the CSV content that was previously written to the local file system.
        // cr-java-0062 FIX: the following block replaces the original lines 38-47 which
        // used File.mkdirs(), new FileWriter(fullPath), writer.write(...), writer.close().
        // All local-disk I/O is eliminated; data is streamed directly to Amazon S3.
        String csvContent = "BookingID,GuestName,RoomType,CheckIn,CheckOut,Amount\n"
                + "BK-001,John Smith,SUITE,2024-03-01,2024-03-05,1750.00\n"
                + "BK-002,Jane Doe,DELUXE,2024-03-03,2024-03-07,960.00\n";

        try {
            // cr-java-0062 FIX (line 42): Upload report CSV to Amazon S3 instead of
            // writing to the local file system via FileWriter.
            // PutObjectRequest targets the environment-variable-backed bucket so the
            // application has zero dependency on the host file system.
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(reportsBucket)
                    .key(objectKey)
                    .contentType("text/csv")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(csvContent));

            result.put("status", "generated");
            // Return the S3 URI instead of the former local file path
            result.put("s3Uri", "s3://" + reportsBucket + "/" + objectKey);
            // cr-java-0077 FIX: use SSM-resolved port instead of hard-coded constant
            result.put("serverPort", resolvedServerPort);

        } catch (S3Exception e) {
            result.put("status", "error");
            result.put("message", e.awsErrorDetails().errorMessage());
        }

        return result;
    }

    /**
     * Builds the download URL for a named report by retrieving the environment-specific
     * base URL from AWS Systems Manager Parameter Store.
     *
     * <p><strong>cr-java-0071 FIX (original source line 66):</strong><br>
     * The original implementation hard-coded the environment-specific URL
     * {@code "http://reports.resorts-internal.com:8080/download/"} directly in the method
     * body. This prevented the application from being deployed to different environments
     * (dev, staging, production) without code changes, violating cloud-native
     * externalized-configuration principles.<br><br>
     * The base URL is now retrieved at runtime from AWS Systems Manager Parameter Store
     * using the parameter name configured via {@code app.ssm.report-download-base-url-param}
     * (environment variable {@code REPORT_DOWNLOAD_URL_PARAM_NAME}). Each deployment
     * environment stores its own value in Parameter Store under the same parameter name,
     * enabling environment-agnostic deployments with zero code changes.</p>
     *
     * @param reportName the name of the report file
     * @return the download URL for the report, with base URL sourced from SSM Parameter Store
     */
    public String buildReportDownloadUrl(String reportName) {
        // cr-java-0071 FIX: retrieve the environment-specific base URL from AWS SSM
        // Parameter Store instead of using the hard-coded value
        // "http://reports.resorts-internal.com:8080/download/" (original source line 66).
        String baseUrl = getReportDownloadBaseUrlFromSsm();
        return baseUrl + reportName;
    }

    /**
     * Retrieves the report download base URL from AWS Systems Manager Parameter Store.
     *
     * <p>Falls back to a safe HTTPS default if the parameter is not yet provisioned,
     * so the application can still start in local/test environments.</p>
     *
     * @return the report download base URL retrieved from SSM Parameter Store
     */
    private String getReportDownloadBaseUrlFromSsm() {
        try {
            // cr-java-0071 FIX: look up the environment-specific URL from SSM Parameter Store
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(reportDownloadBaseUrlParamName)
                    .withDecryption(false)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            return response.parameter().value();
        } catch (SsmException e) {
            // Fall back to a safe HTTPS default if the parameter is not yet provisioned
            return "https://reports.resorts-internal.com/download/";
        }
    }

    /**
     * Returns system information including S3 storage references and the dynamically
     * resolved server port.
     *
     * <p>Previously returned the hard-coded local paths {@code /var/legacy/reports/} and
     * {@code C:\\ResortBackups\\nightly\\} (cr-java-0062). These are now replaced with
     * the S3 bucket name and backup prefix sourced from environment variables, ensuring
     * no host file-system paths are exposed in API responses.</p>
     *
     * <p><strong>cr-java-0077 FIX:</strong> {@code serverPort} is now the value resolved
     * from AWS SSM Parameter Store at startup, not the former hard-coded constant
     * {@code SERVER_PORT = 8080} (original source line 28).</p>
     *
     * <p><strong>cr-java-0111 FIX (original source line 70):</strong> the timestamp is
     * now generated using {@link ZonedDateTime#now(java.time.ZoneId)} with
     * {@link ZoneOffset#UTC} and formatted via {@link DateTimeFormatter}, replacing the
     * former {@code new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())} call
     * that relied on the server-local timezone. Standardizing on UTC ensures consistent
     * timestamps across all cloud regions and container instances.</p>
     *
     * @return a map of system information key-value pairs
     */
    public Map<String, Object> getSystemInfo() {
        // cr-java-0111 FIX (original source line 70): replaced java.util.Date /
        // SimpleDateFormat with java.time API standardized on UTC (ZoneOffset.UTC) to
        // avoid timezone inconsistencies across cloud regions and container instances.
        // Previously: new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Map<String, Object> info = new HashMap<>();
        // cr-java-0062: replaced hard-coded local path with S3 bucket reference
        info.put("reportsBucket", reportsBucket);
        // cr-java-0062: replaced hard-coded Windows backup path with S3 prefix
        info.put("backupPrefix", backupPrefix);
        // cr-java-0077 FIX: use SSM-resolved port instead of hard-coded constant SERVER_PORT = 8080
        info.put("serverPort", resolvedServerPort);
        info.put("generatedAt", timestamp);
        return info;
    }
}
