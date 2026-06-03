# ResortsLite - Cloud-Ready Application for AWS

## Overview
This application has been transformed to be fully cloud-ready for AWS deployment. All cloud compatibility blockers have been resolved.

## Cloud Readiness Fixes Applied

### 1. File System Dependencies (cr-java-0061, cr-java-0062, cr-java-0063)
- **Issue**: Hard-coded file paths and local file system operations
- **Fix**: Migrated to Amazon S3 for all file storage operations
- **Configuration**: Set `S3_REPORTS_BUCKET` and `S3_BACKUPS_BUCKET` environment variables

### 2. Hard-coded Database Credentials (cr-java-0069)
- **Issue**: Database credentials embedded in source code
- **Fix**: Integrated AWS Secrets Manager for secure credential retrieval
- **Configuration**: Store credentials in AWS Secrets Manager with secret name from `AWS_SECRET_DB_NAME`

### 3. Hard-coded Environment URLs (cr-java-0071)
- **Issue**: Environment-specific URLs hard-coded in application
- **Fix**: Externalized to AWS Systems Manager Parameter Store
- **Configuration**: Set environment variables or Parameter Store values for service endpoints

### 4. Hard-coded Ports (cr-java-0077)
- **Issue**: Fixed port numbers preventing dynamic assignment
- **Fix**: Port configuration via `SERVER_PORT` environment variable
- **Configuration**: ECS/EKS can now dynamically assign ports

### 5. HTTP Session State Storage (cr-java-0065)
- **Issue**: Session data stored in local memory, breaking horizontal scaling
- **Fix**: Migrated to Amazon ElastiCache for Redis with Spring Session
- **Configuration**: Set `REDIS_HOST` and `REDIS_PORT` for ElastiCache endpoint

### 6. In-Memory Caching Without TTL (cr-java-0067)
- **Issue**: Unbounded in-memory cache causing memory issues
- **Fix**: Replaced with Redis-based distributed caching with 30-minute TTL
- **Configuration**: Uses same Redis configuration as session management

### 7. File-based Authentication (cr-java-0090)
- **Issue**: Authentication credentials stored in local files
- **Fix**: Integrated AWS Secrets Manager and prepared for Amazon Cognito
- **Configuration**: Use Cognito for user authentication in production

### 8. Clock/Time Dependencies (cr-java-0111)
- **Issue**: Using java.util.Date with local timezone
- **Fix**: Migrated to java.time API with UTC standardization
- **Impact**: All timestamps now use UTC for consistency across regions

## AWS Services Required

### Core Services
1. **Amazon ElastiCache for Redis**
   - Purpose: Distributed session management and caching
   - Configuration: Provide endpoint via `REDIS_HOST` and `REDIS_PORT`

2. **Amazon S3**
   - Purpose: File storage for reports and backups
   - Buckets: Configure via `S3_REPORTS_BUCKET` and `S3_BACKUPS_BUCKET`

3. **AWS Secrets Manager**
   - Purpose: Secure credential storage
   - Secret: Create secret with database credentials (host, username, password)

4. **AWS Systems Manager Parameter Store**
   - Purpose: Externalized configuration management
   - Parameters: Service endpoints, feature flags, etc.

### Optional Services
5. **Amazon Cognito** (recommended for production)
   - Purpose: User authentication and authorization
   - Integration: Replace file-based authentication

6. **Amazon RDS** (for production database)
   - Purpose: Managed relational database
   - Configuration: Update `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`

## Environment Variables

### Required
```bash
# AWS Configuration
AWS_REGION=us-east-1
AWS_SECRET_DB_NAME=resorts-db-credentials

# Redis/ElastiCache
REDIS_HOST=your-elasticache-endpoint.cache.amazonaws.com
REDIS_PORT=6379

# S3 Buckets
S3_REPORTS_BUCKET=resorts-reports-bucket
S3_BACKUPS_BUCKET=resorts-backups-bucket

# Server Configuration
SERVER_PORT=8080
```

### Optional
```bash
# Database (if not using Secrets Manager)
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/resorts
DB_USERNAME=admin
DB_PASSWORD=secure-password

# Service Endpoints
PAYMENT_ENDPOINT=https://payment-svc.internal:9090/charge
INVENTORY_ENDPOINT=https://inventory-svc.internal:8081/rooms/available
NOTIFICATION_ENDPOINT=https://notify.internal:7070/send
```

## AWS IAM Permissions Required

The application requires the following IAM permissions:

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
        "arn:aws:s3:::resorts-reports-bucket/*",
        "arn:aws:s3:::resorts-backups-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:*:*:secret:resorts-db-credentials-*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:*:*:parameter/resorts/*"
    }
  ]
}
```

## Deployment Options

### 1. Amazon ECS (Elastic Container Service)
- Recommended for containerized deployment
- Supports dynamic port mapping
- Integrates with Application Load Balancer

### 2. Amazon EKS (Elastic Kubernetes Service)
- Recommended for Kubernetes-based deployment
- Full orchestration capabilities
- Service mesh integration available

### 3. AWS Elastic Beanstalk
- Simplified deployment option
- Automatic scaling and load balancing
- Managed platform updates

## Local Development

For local development without AWS services:

```bash
# Use H2 in-memory database
DB_URL=jdbc:h2:mem:resortdb

# Use local Redis (Docker)
docker run -d -p 6379:6379 redis:latest
REDIS_HOST=localhost
REDIS_PORT=6379

# Mock AWS services (optional)
# Use LocalStack for local AWS service emulation
```

## Health Checks

The application exposes health check endpoints for container orchestration:

- **Health**: `GET /actuator/health`
- **Info**: `GET /actuator/info`
- **Metrics**: `GET /actuator/metrics`

Configure your load balancer to use `/actuator/health` for health checks.

## Security Considerations

1. **Never commit credentials**: All credentials are externalized
2. **Use IAM roles**: Assign IAM roles to ECS tasks or EKS pods
3. **Enable encryption**: Use encryption at rest for S3 and ElastiCache
4. **Network security**: Deploy in private subnets with security groups
5. **Secrets rotation**: Enable automatic rotation in Secrets Manager

## Monitoring and Logging

- Application logs are written to stdout/stderr for CloudWatch Logs
- All timestamps use UTC for consistency
- Structured logging format for easy parsing
- Metrics available via Spring Boot Actuator

## Next Steps

1. Create required AWS resources (S3 buckets, ElastiCache cluster, Secrets Manager secrets)
2. Configure IAM roles with required permissions
3. Set environment variables in your deployment platform
4. Deploy application to ECS, EKS, or Elastic Beanstalk
5. Configure Application Load Balancer with health checks
6. Set up CloudWatch alarms for monitoring

## Support

For issues or questions about cloud deployment, refer to AWS documentation:
- [Amazon ECS Documentation](https://docs.aws.amazon.com/ecs/)
- [Amazon ElastiCache Documentation](https://docs.aws.amazon.com/elasticache/)
- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
