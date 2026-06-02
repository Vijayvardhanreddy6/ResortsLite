# ResortsLite - Cloud-Native Migration

## Overview
This application has been migrated to be fully cloud-ready for AWS deployment. All cloud compatibility blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System & Storage (cr-java-0061, cr-java-0062, cr-java-0063)
**Issue**: Hard-coded file paths and local file system dependencies
**Solution**: Migrated to Amazon S3 for all file storage operations
- Report generation now writes directly to S3
- Removed dependencies on `/var/legacy/reports/` and `C:\ResortBackups\`
- All file operations use AWS SDK for Java v2 S3 client

### 2. Configuration Management (cr-java-0069, cr-java-0071)
**Issue**: Hard-coded database credentials and environment URLs
**Solution**: Externalized all configuration to AWS services
- Database credentials stored in AWS Secrets Manager
- Service endpoints configured via AWS Systems Manager Parameter Store
- All configuration values use environment variables with sensible defaults

### 3. Networking & Communication (cr-java-0077)
**Issue**: Hard-coded port numbers preventing dynamic assignment
**Solution**: Port configuration externalized to environment variables
- Server port configurable via `SERVER_PORT` environment variable
- Supports dynamic port assignment by ECS/EKS/Elastic Beanstalk

### 4. State Management & Session (cr-java-0065, cr-java-0067)
**Issue**: HTTP session storage and in-memory caching without TTL
**Solution**: Migrated to Amazon ElastiCache for Redis
- Spring Session with Redis for distributed session management
- RedisTemplate for distributed caching with TTL policies
- Stateless application instances supporting horizontal scaling

### 5. Security & Authentication (cr-java-0090)
**Issue**: File-based authentication storage
**Solution**: Migrated to AWS Secrets Manager
- Authentication tokens stored in Secrets Manager
- Centralized, encrypted credential storage
- Support for automatic credential rotation

### 6. Time Dependencies (cr-java-0111)
**Issue**: Use of java.util.Date and local timezone dependencies
**Solution**: Migrated to java.time API with UTC standardization
- All timestamps use `Instant` and `ZonedDateTime`
- Standardized on UTC timezone across all services
- ISO-8601 formatted timestamps for consistency

## AWS Services Required

### Core Services
- **Amazon S3**: Object storage for reports and file operations
- **Amazon ElastiCache for Redis**: Distributed session and cache storage
- **AWS Secrets Manager**: Secure credential and authentication token storage
- **AWS Systems Manager Parameter Store**: Configuration parameter management

### Database
- **Amazon RDS** (recommended for production): Replace H2 with RDS PostgreSQL/MySQL
- Current H2 configuration maintained for development/testing

## Environment Variables

### Required Configuration
```bash
# Server Configuration
SERVER_PORT=8080

# Database Configuration (use Secrets Manager in production)
DB_URL=jdbc:h2:mem:resortdb
DB_USERNAME=sa
DB_PASSWORD=
DB_DRIVER=org.h2.Driver

# Redis Configuration (ElastiCache endpoint)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# AWS Configuration
AWS_REGION=us-east-1
S3_BUCKET_NAME=resortslite-reports
DB_SECRET_NAME=resortslite/db/credentials

# Service Endpoints
PAYMENT_ENDPOINT=http://payment-svc:9090/charge
INVENTORY_ENDPOINT=http://inventory-svc:8081/rooms
NOTIFICATION_ENDPOINT=http://notify-svc:7070/send
```

### Optional Configuration
```bash
# Database Connection Pool
DB_POOL_SIZE=10
DB_POOL_MIN_IDLE=5
DB_CONNECTION_TIMEOUT=30000

# Session Configuration
SESSION_TIMEOUT=1800

# Storage Configuration
STORAGE_TYPE=s3
S3_REPORTS_PREFIX=reports/
S3_BACKUPS_PREFIX=backups/

# Time Zone
APP_TIMEZONE=UTC
```

## Deployment Checklist

### Pre-Deployment
1. ✅ Create S3 bucket for report storage
2. ✅ Set up ElastiCache for Redis cluster
3. ✅ Create Secrets Manager secrets for database credentials
4. ✅ Configure Parameter Store parameters for service endpoints
5. ✅ Set up IAM roles with appropriate permissions:
   - S3 read/write access
   - Secrets Manager read access
   - Parameter Store read access
   - ElastiCache access

### IAM Policy Requirements
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::resortslite-reports",
        "arn:aws:s3:::resortslite-reports/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:resortslite/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/resortslite/*"
    }
  ]
}
```

## Architecture Benefits

### Scalability
- Stateless application instances enable horizontal scaling
- Distributed session management supports multi-instance deployments
- S3 storage eliminates local disk dependencies

### Reliability
- Durable storage with S3 (99.999999999% durability)
- High availability with ElastiCache Redis replication
- Automatic credential rotation with Secrets Manager

### Security
- Encrypted credential storage in Secrets Manager
- No credentials in source code or container images
- IAM-based access control for all AWS services

### Maintainability
- Externalized configuration for environment-specific values
- Consistent time handling with UTC standardization
- Cloud-native patterns following 12-factor app principles

## Migration from Legacy

### Removed Dependencies
- Local file system paths (`/var/legacy/reports/`, `C:\ResortBackups\`)
- Hard-coded database credentials
- HTTP session-based state storage
- In-memory caching without TTL
- java.util.Date and timezone dependencies

### Added Dependencies
- AWS SDK for Java v2 (S3, Secrets Manager, SSM)
- Spring Session Data Redis
- Spring Boot Data Redis
- Lettuce Redis client

## Testing

### Local Development
For local development without AWS services:
1. Use H2 in-memory database (default configuration)
2. Run Redis locally via Docker: `docker run -p 6379:6379 redis:alpine`
3. Set environment variables to use local endpoints

### Integration Testing
For testing with AWS services:
1. Configure AWS credentials (IAM role or access keys)
2. Create test S3 bucket and ElastiCache cluster
3. Set up test Secrets Manager secrets
4. Run application with AWS-specific environment variables

## Support

For issues or questions regarding the cloud migration:
- Review AWS service documentation
- Check CloudWatch logs for runtime errors
- Verify IAM permissions for service access
- Ensure all required environment variables are set

## Version History

### v1.0.0 - Cloud-Native Migration
- Migrated file storage to Amazon S3
- Implemented distributed session management with Redis
- Externalized all configuration to AWS services
- Replaced hard-coded credentials with Secrets Manager
- Standardized time handling with java.time API
- Added comprehensive cloud-native configuration
