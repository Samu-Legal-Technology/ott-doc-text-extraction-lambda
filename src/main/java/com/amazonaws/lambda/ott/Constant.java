package com.amazonaws.lambda.ott;

public final class Constant {
	public final static String snsTopicArn="arn:aws:sns:us-east-1:371292405073:AmazonTextractOttPoc";
	public final static String roleArn="arn:aws:iam::371292405073:role/ott-poc-role";
	public final static String sqsUrl="https://sqs.us-east-1.amazonaws.com/371292405073/S3ToSQSQueue";
	final static String lambdaFunctionInvokedLogger="Lambda Function invoked";
	final static String messageBodyLogger="Message body is ";
	final static String records="Records";
	final static String record="Records";
	final static String s3Constant="s3";
	final static String bucket ="bucket";
	final static String name="name";
	final static String objectConstant="object";
	final static String keyConstant="key";
	final static String bucketNameLogger="bucket name is ";
	final static String keyNameLogger="key is ";
	public final static String splitter="/";
	final static String clientCase="client";
	final static String correspondenceCase="correspondence";
	final static String medicalCase="medical";

	}
