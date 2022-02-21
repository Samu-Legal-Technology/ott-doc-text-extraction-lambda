package com.amazonaws.lambda.ott;

import org.json.*;

import com.amazonaws.lambda.ott.service.TextractDocumentAnalysisService;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.model.LimitExceededException;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;
import com.amazonaws.services.textract.model.DocumentLocation;
import com.amazonaws.services.textract.model.NotificationChannel;
import com.amazonaws.services.textract.model.S3Object;

import com.amazonaws.services.textract.model.StartDocumentTextDetectionRequest;


import static com.amazonaws.lambda.ott.Constant.roleArn;
import static com.amazonaws.lambda.ott.Constant.snsTopicArn;
import static com.amazonaws.lambda.ott.Constant.sqsUrl;


import static com.amazonaws.lambda.ott.Constant.lambdaFunctionInvokedLogger;
import static com.amazonaws.lambda.ott.Constant.messageBodyLogger;
import static com.amazonaws.lambda.ott.Constant.bucket;
import static com.amazonaws.lambda.ott.Constant.name;
import static com.amazonaws.lambda.ott.Constant.objectConstant;
import static com.amazonaws.lambda.ott.Constant.keyConstant;
import static com.amazonaws.lambda.ott.Constant.records;
import static com.amazonaws.lambda.ott.Constant.s3Constant;
import static com.amazonaws.lambda.ott.Constant.bucketNameLogger;
import static com.amazonaws.lambda.ott.Constant.clientCase;
import static com.amazonaws.lambda.ott.Constant.medicalCase;
import static com.amazonaws.lambda.ott.Constant.correspondenceCase;
import static com.amazonaws.lambda.ott.Constant.keyNameLogger;
import static com.amazonaws.lambda.ott.Constant.splitter;

public class LambdaFunctionHandler implements RequestHandler<SQSEvent, String> {

	JSONObject responseJson = null;
	String messageBody = null;

//	private static Logger logger = LoggerFactory.getLogger(LambdaFunctionHandler.class);

	@Override
	public String handleRequest(SQSEvent input, Context context) {
		try {
			LambdaLogger logger = context.getLogger();
			logger.log(lambdaFunctionInvokedLogger);
			AmazonTextract client = AmazonTextractClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
						
			for (SQSMessage message : input.getRecords()) {
				JSONObject json = new JSONObject(message.getBody());
				logger.log(messageBodyLogger + json.toString());
				if (!json.isNull(records)) {
					messageBody = json.toString();
					JSONArray records = json.getJSONArray(Constant.records);

					for (int i = 0; i < records.length(); i++) {
						JSONObject object = records.getJSONObject(i);

						JSONObject s3 = object.getJSONObject(s3Constant);
						String bucketName = s3.getJSONObject(bucket).getString(name);
						logger.log(bucketNameLogger + bucketName);
						String key = s3.getJSONObject(objectConstant).getString(keyConstant);
						String []split = key.split(splitter);
						logger.log(keyNameLogger + key);
						NotificationChannel channel = new NotificationChannel().withSNSTopicArn(snsTopicArn)
								.withRoleArn(roleArn);
						String jobTag = split[1];
						StartDocumentTextDetectionRequest detectionRequest = null;
						logger.log("Document type : "+split[1]);
						switch (split[1]) {
						case clientCase:
							detectionRequest = new StartDocumentTextDetectionRequest();

							detectionRequest
									.withDocumentLocation(new DocumentLocation()
											.withS3Object(new S3Object().withName(key).withBucket(bucketName)))
									.withNotificationChannel(channel).withJobTag(jobTag);
							client.startDocumentTextDetection(detectionRequest);

							break;
						case correspondenceCase:
							TextractDocumentAnalysisService.textractDocumentAnalysis.accept(bucketName, key);
							break;

						case medicalCase:

							TextractDocumentAnalysisService.textractDocumentAnalysis.accept(bucketName, key);
							break;
							
						default:
							break;

						}

					}
				}
			}

		} catch (ProvisionedThroughputExceededException | LimitExceededException exception) {
			AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
			sqs.sendMessage(new SendMessageRequest().withQueueUrl(sqsUrl)
					.withMessageBody(messageBody).withDelaySeconds(5));
		}

		return "";
	}

}