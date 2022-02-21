package com.amazonaws.lambda.ott.service;

import static com.amazonaws.lambda.ott.Constant.roleArn;
import static com.amazonaws.lambda.ott.Constant.snsTopicArn;
import static com.amazonaws.lambda.ott.Constant.splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.services.textract.model.NotificationChannel;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.DocumentLocation;
import software.amazon.awssdk.services.textract.model.FeatureType;
import software.amazon.awssdk.services.textract.model.S3Object;
import software.amazon.awssdk.services.textract.model.StartDocumentAnalysisRequest;
import software.amazon.awssdk.services.textract.model.StartDocumentAnalysisResponse;

public class TextractDocumentAnalysisService {

	
	private static Logger logger = LoggerFactory.getLogger(TextractDocumentAnalysisService.class);
	
	
	private TextractDocumentAnalysisService() {
		
	}
	
	public static final BiConsumer<String, String> textractDocumentAnalysis=( bucketName, key) -> {
		try {
			List<FeatureType> myList = new ArrayList<>();
			myList.add(FeatureType.TABLES);
			myList.add(FeatureType.FORMS);
			
			
			NotificationChannel notificationChannel = NotificationChannel.builder().snsTopicArn(snsTopicArn)
					.roleArn(roleArn)
					.build();
			
			S3Object s3Object = S3Object
					.builder().bucket(bucketName).name(key).build();

			DocumentLocation location = DocumentLocation
					.builder().s3Object(s3Object).build();

			StartDocumentAnalysisRequest documentAnalysisRequest = software.amazon.awssdk.services.textract.model.StartDocumentAnalysisRequest
					.builder().documentLocation(location).featureTypes(myList)
					.notificationChannel(notificationChannel)
					.jobTag(key.split(splitter)[1])
					.build();

			TextractClient textractClient = TextractClient.builder().region(Region.US_EAST_1).build();

			StartDocumentAnalysisResponse response = textractClient
					.startDocumentAnalysis(documentAnalysisRequest);
			logger.info("{}",response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	};
}
