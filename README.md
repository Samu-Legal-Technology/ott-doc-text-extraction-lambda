# OTT Document Text Extraction Lambda

## Overview

This AWS Lambda function serves as an intelligent document processing router that automatically triggers AWS Textract jobs based on document type. It processes S3 events via SQS, classifies documents by their S3 path prefix, and initiates appropriate Textract analysis - simple text extraction for client documents or advanced form/table analysis for correspondence and medical documents.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  S3 Bucket      │────▶│  S3 Events      │────▶│   SQS Queue     │────▶│     Lambda      │
│  (Documents)    │     └─────────────────┘     │ (S3ToSQSQueue)  │     │   (Router)      │
└─────────────────┘                             └─────────────────┘     └────────┬────────┘
                                                                                  │
                          Document Type Routing                                   │
                    ┌─────────────────────────────────────────────────────────────┴────────┐
                    │                                   │                                   │
                    ▼                                   ▼                                   ▼
            ┌──────────────┐                   ┌──────────────┐                   ┌──────────────┐
            │ Client Docs  │                   │Correspondence│                   │Medical Docs  │
            │   (Text)     │                   │(Forms/Tables)│                   │(Forms/Tables)│
            └──────┬───────┘                   └──────┬───────┘                   └──────┬───────┘
                   │                                   │                                   │
                   └───────────────────────────────────┴───────────────────────────────────┘
                                                       │
                                                       ▼
                                              ┌────────────────┐
                                              │    Textract    │
                                              │ (Async Jobs)   │
                                              └────────┬───────┘
                                                       │
                                                       ▼
                                              ┌────────────────┐
                                              │   SNS Topic    │
                                              │ (Completion)   │
                                              └────────────────┘
```

## Features

### Document Classification
- **Automatic Type Detection**: Based on S3 path prefixes
- **Client Documents**: Simple text extraction
- **Correspondence Documents**: Form and table analysis
- **Medical Documents**: Form and table analysis with medical context

### Processing Capabilities
- **Batch Processing**: Handles multiple documents per Lambda invocation
- **Asynchronous Operation**: Non-blocking Textract job initiation
- **Error Resilience**: Automatic retry with rate limit handling
- **Job Tracking**: Document type tagging for downstream processing

### Integration Points
- **S3 Event Driven**: Triggered automatically on document upload
- **SQS Queue**: Reliable message delivery and retry
- **SNS Notifications**: Job completion alerts
- **Textract APIs**: Both simple and advanced analysis

## Technical Stack

- **Java 8**: Core programming language
- **AWS Lambda**: Serverless compute
- **AWS SDK**: Mixed v1 and v2 for different services
- **Maven**: Build and dependency management
- **SQS**: Message queue for S3 events
- **SNS**: Notification service for job completion

## Prerequisites

- Java 8 or higher
- Maven 3.6+
- AWS Account with appropriate permissions
- S3 bucket with event notifications configured
- SQS queue for event delivery
- SNS topic for Textract notifications

## Installation & Build

1. Clone the repository:
```bash
git clone https://github.com/Samu-Legal-Technology/ott-doc-text-extraction-lambda.git
cd ott-doc-text-extraction-lambda
```

2. Build the project:
```bash
mvn clean package
```

3. The Lambda deployment package will be created at:
```
target/ott-doc-text-extraction-lambda-0.0.1-SNAPSHOT.jar
```

## Configuration

### Lambda Environment

The following resources are configured in code (should be externalized):

| Resource | Current Value | Purpose |
|----------|---------------|---------|
| SNS Topic ARN | `arn:aws:sns:us-east-1:371292405073:AmazonTextractOttPoc` | Job completion notifications |
| IAM Role ARN | `arn:aws:iam::371292405073:role/ott-poc-role` | Textract service role |
| SQS Queue URL | `https://sqs.us-east-1.amazonaws.com/371292405073/S3ToSQSQueue` | Source queue |
| AWS Region | `us-east-1` | Default region |

### Document Type Configuration

Documents are classified by S3 key prefix:
- `client/` → Simple text detection
- `correspondence/` → Advanced analysis (forms/tables)
- `medical/` → Advanced analysis (forms/tables)

## Deployment

### Lambda Function Setup

1. Create Lambda function in AWS Console
2. Runtime: Java 8 (Corretto)
3. Handler: `com.amazonaws.services.lambda.runtime.events.handler.LambdaFunctionHandler::handleRequest`
4. Memory: 512 MB
5. Timeout: 1 minute
6. Upload JAR from target directory

### IAM Permissions

Lambda execution role requires:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "textract:StartDocumentTextDetection",
        "textract:StartDocumentAnalysis"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::*/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:SendMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:*:*:S3ToSQSQueue"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sns:Publish"
      ],
      "Resource": "arn:aws:sns:*:*:AmazonTextractOttPoc"
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": "arn:aws:iam::*:role/ott-poc-role"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
```

### S3 Event Configuration

1. Configure S3 bucket to send events to SQS:
```
Event types: s3:ObjectCreated:*
Prefix filters: client/, correspondence/, medical/
```

2. Add SQS as Lambda trigger:
```
Batch size: 10
Maximum batching window: 0 seconds
```

## Usage

### Document Upload Workflow

1. **Upload Document** to S3 in appropriate folder:
   - Legal contracts → `s3://bucket/correspondence/contract.pdf`
   - Client forms → `s3://bucket/client/application.pdf`
   - Medical records → `s3://bucket/medical/patient-record.pdf`

2. **Automatic Processing**:
   - S3 event triggers SQS message
   - Lambda processes message and starts Textract job
   - SNS notification sent on completion

3. **Downstream Processing**:
   - Another Lambda can process Textract results
   - Results stored in database or search index

### Monitoring Job Status

Track Textract jobs using the JobTag:
- `correspondence` - Forms and tables analysis
- `medical` - Medical document analysis
- `client` - Simple text extraction

## Error Handling

### Rate Limiting
The Lambda implements exponential backoff for `ProvisionedThroughputExceededException`:
```java
// Waits 1 second then re-queues message
Thread.sleep(1000);
sqsClient.sendMessage(queueUrl, message);
```

### Dead Letter Queue
Configure DLQ on SQS for messages that fail repeatedly.

## Performance Optimization

1. **Batch Size**: Adjust SQS batch size based on document volume
2. **Memory Allocation**: Increase Lambda memory for faster processing
3. **Concurrent Executions**: Set reserved concurrency to prevent throttling
4. **Queue Configuration**: Use long polling and appropriate visibility timeout

## Monitoring & Debugging

### CloudWatch Metrics
- Lambda invocations and errors
- SQS message age and count
- Textract job initiation success rate

### Logging
Enable debug logging:
```java
System.out.println("Processing document: " + s3Key);
System.out.println("Document type: " + documentType);
System.out.println("Textract JobId: " + jobId);
```

### Common Issues
1. **Access Denied**: Check IAM permissions
2. **Throttling**: Implement better retry logic
3. **Message Duplication**: Ensure idempotent processing
4. **Large Files**: Increase Lambda timeout

## Best Practices

1. **Environment Variables**: Move hardcoded values to Lambda environment
2. **Error Handling**: Implement comprehensive exception handling
3. **Monitoring**: Add custom CloudWatch metrics
4. **Testing**: Add unit tests for document classification logic
5. **Documentation**: Keep document type mappings updated

## Future Enhancements

- [ ] Support for additional document types
- [ ] Dynamic configuration via DynamoDB
- [ ] Intelligent document classification using ML
- [ ] Multi-region support
- [ ] Direct integration with document management systems
- [ ] Support for batch document uploads
- [ ] Real-time processing status updates
- [ ] Cost optimization through intelligent job batching

## Testing

### Local Testing
```java
// Create test SQS event
SQSEvent testEvent = new SQSEvent();
SQSMessage message = new SQSMessage();
message.setBody("{\"Records\":[{\"s3\":{\"bucket\":{\"name\":\"test-bucket\"},\"object\":{\"key\":\"correspondence/test.pdf\"}}}]}");
testEvent.setRecords(Arrays.asList(message));

// Invoke handler
new LambdaFunctionHandler().handleRequest(testEvent, null);
```

### Integration Testing
1. Upload test documents to S3
2. Verify SQS messages are processed
3. Check Textract job initiation
4. Validate SNS notifications

## Contributing

1. Fork the repository
2. Create feature branch
3. Add tests for new functionality
4. Submit pull request with detailed description

## License

Copyright © 2024 Samu Legal Technology. All rights reserved.

---

*Maintained by Samu Legal Technology Development Team*