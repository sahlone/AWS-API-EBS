package com.zycus.eproc.credentials;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkAsyncClient;
import com.amazonaws.services.elasticbeanstalk.model.AWSElasticBeanstalkException;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionStatus;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityRequest;
import com.amazonaws.services.elasticbeanstalk.model.CheckDNSAvailabilityResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentRequest;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateStorageLocationResult;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DeleteApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationVersionsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentHealthRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentHealthResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEventsResult;
import com.amazonaws.services.elasticbeanstalk.model.ElasticBeanstalkServiceException;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentHealthAttribute;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentHealthStatus;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentStatus;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;
import com.amazonaws.services.elasticbeanstalk.model.EventSeverity;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.TerminateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.lambda.AWSLambdaAsyncClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;

import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import com.amazonaws.handlers.AsyncHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class AWSConfigureAsync
{
	private static String		AWS_SOLUTION_STACK						= "64bit Amazon Linux 2016.03 v2.2.0 running Tomcat 7 Java 7";
	//private static String		AWS_SOLUTION_STACK						= "64bit Amazon Linux 2016.03 v2.1.6 running PHP 5.5";

	private static String		EPROC_APPLICATION_SETUP					= "PROD";
	private static String		AWS_APPLICATION_NAME					= "EPROC-" + EPROC_APPLICATION_SETUP;
	private static String		AWS_ENVIRONMENT_DESCRIPTION				= "eProc Deployment ";
	private static String		APPLICATION_VERSION						= "$RELEASE_NO$";
	private static String		AWS_APPLICATION_ENVIRONMENT_NAME		= AWS_APPLICATION_NAME + "-"
																				+ APPLICATION_VERSION;

	private static String		AWS_ACCESS_KEY_ID						= "eProc";
	private static String		AWS_SECRET_ACCESS_KEY					= "eProc";
	private static String		AWS_CREDENTIAL_PROFILES_FILE			= "eProc";

	private static String		AWS_S3_KEY								= AWS_APPLICATION_NAME + "/releases/"
																				+ APPLICATION_VERSION;
	private static Region		AWS_REGION								= null;

	private static String		CNAME_PREFIX							= AWS_APPLICATION_ENVIRONMENT_NAME;

	private static final long	sleepTimeForOperationsinMilliSeconds	= 10000;
	private static final String	AWS_APPLICATION_DESCRIPTION				= "EPROC APPLICATION";
	private static final long	MAX_STATUS_UPDATE_TIME					= 1200L;
	private static String		AWS_SOURCE_BUNDLE						= "";

	public static void main(String[] args)
	{
		try
		{
			AWSConfigureAsync configure = new AWSConfigureAsync();
			String access_key_id = args[0];
			String secret_access_key = args[1];
			String userProvidedRegion = args[2];
			String environment = args[3];
			String releaseNo = args[4];
			String cnamePrefix = "";
			DeploymentResult deploymentResult = configure.createApplicationAndEnvironment(access_key_id,
					secret_access_key, userProvidedRegion, environment, releaseNo, cnamePrefix);

			GsonBuilder builder = new GsonBuilder();

			Gson gson = builder.setPrettyPrinting().disableHtmlEscaping().create();
			String result = gson.toJson(deploymentResult);
			result = escapeAWSChars(result);
			System.out.print(result);
			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/elasticbeanstalk/AWSElasticBeanstalk.html
	 * @param access_key_id
	 * @param secret_access_key
	 * @param userProvidedRegion
	 * @param releaseNo
	 * @throws InterruptedException 
	 */
	public DeploymentResult createApplicationAndEnvironment(String access_key_id, String secret_access_key,
			String userProvidedRegion, String applicationSetup, String releaseNo, String cnamePrefix)
			throws InterruptedException
	{
		DeploymentResult deploymentResult = new DeploymentResult();
		try
		{
			java.security.Security.setProperty("networkaddress.cache.ttl", "60");

			AWSConfigureAsync.AWS_ACCESS_KEY_ID = access_key_id;
			AWSConfigureAsync.AWS_SECRET_ACCESS_KEY = secret_access_key;
			AWSConfigureAsync.EPROC_APPLICATION_SETUP = applicationSetup;
			AWSConfigureAsync.AWS_APPLICATION_NAME = "EPROC-" + EPROC_APPLICATION_SETUP;
			AWSConfigureAsync.APPLICATION_VERSION = escapeAWSChars(releaseNo);
			AWS_ENVIRONMENT_DESCRIPTION = AWS_APPLICATION_NAME + "-" + APPLICATION_VERSION;
			AWS_APPLICATION_ENVIRONMENT_NAME = APPLICATION_VERSION + "-" + getCurrentTimeStamp();
			AWS_S3_KEY = AWS_APPLICATION_NAME + "/releases/" + APPLICATION_VERSION;
			if (StringUtils.isBlank(cnamePrefix))
			{

				CNAME_PREFIX = AWS_APPLICATION_ENVIRONMENT_NAME;
			}
			else
			{
				CNAME_PREFIX = escapeAWSChars(cnamePrefix);
			}

			BasicAWSCredentials awsCreds = new BasicAWSCredentials(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY);
			String operationalRegion = escapeAWSChars(userProvidedRegion.toLowerCase());
			try
			{
				AWS_REGION = Region.getRegion(Regions.fromName(operationalRegion));
			}
			catch (IllegalArgumentException e)
			{
				throw e;
			}

			if (!AWS_REGION.isServiceSupported(AWSElasticBeanstalk.ENDPOINT_PREFIX))
			{
				throw new EprocAWSException(
						"Region provided for operations does not support elastic beanstalk service.Please provide a region which supports elastic beanstalk.\n Provided region for operations :"
								+ userProvidedRegion);
			}
			//AWSElasticBeanstalkClient ebsClient = new AWSElasticBeanstalkClient(awsCreds);
			AWSElasticBeanstalkAsyncClient ebsClient = new AWSElasticBeanstalkAsyncClient(awsCreds);
			ebsClient.setRegion(AWS_REGION);
			ebsClient.setEndpoint("https://elasticbeanstalk." + userProvidedRegion + ".amazonaws.com");
			AmazonS3Client amazonS3Client = new AmazonS3Client(awsCreds).withRegion(AWS_REGION);
			//Check DNS Availibility For envionment

			while (true)
			{

				CheckDNSAvailabilityRequest request = new CheckDNSAvailabilityRequest().withCNAMEPrefix(CNAME_PREFIX);
				CheckDNSAvailabilityResult checkDNSAvailabilityResult = ebsClient.checkDNSAvailability(request);
				if (checkDNSAvailabilityResult.getAvailable())
				{
					break;
				}
				else
				{
					CNAME_PREFIX = AWS_APPLICATION_ENVIRONMENT_NAME + UUID.randomUUID().toString().substring(0, 62);
				}
			}
			deploymentResult.setEnvironmentDNSNamePrefix(CNAME_PREFIX);
			//Verify application Settings

			//make configuration settings validate and delete

			/*	ValidateConfigurationSettingsRequest validateConfigurationSettingsRequest = new ValidateConfigurationSettingsRequest()
						.withApplicationName(AWS_APPLICATION_NAME).withEnvironmentName(AWS_APPLICATION_ENVIRONMENT_NAME);
				ValidateConfigurationSettingsResult response = ebsClient
						.validateConfigurationSettings(validateConfigurationSettingsRequest);*/

			//Create Application

			DescribeApplicationsRequest describeApplicationsRequest = new DescribeApplicationsRequest()
					.withApplicationNames(AWS_APPLICATION_NAME);
			Future<DescribeApplicationsResult> describeApplicationsResult = ebsClient
					.describeApplicationsAsync(describeApplicationsRequest);

			while (!describeApplicationsResult.isDone())
			{
				Thread.sleep(sleepTimeForOperationsinMilliSeconds);
			}

			if (describeApplicationsResult.get().getApplications().size() < 1)
			{

				CreateApplicationRequest applicationRequest = new CreateApplicationRequest().withApplicationName(
						AWS_APPLICATION_NAME).withDescription(AWS_APPLICATION_DESCRIPTION);
				Future<CreateApplicationResult> createApplicationResult = ebsClient
						.createApplicationAsync(applicationRequest);
				while (!createApplicationResult.isDone())
				{
					Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				}
				deploymentResult.setCreateApplicationResult(createApplicationResult.get());
				if (createApplicationResult.get().getApplication() == null)
				{
					throw new EprocAWSException("Application Creation unsuccessfull");
				}
			}

			//Create Storage Location for current version for current App.

			CreateStorageLocationResult location = ebsClient.createStorageLocation();
			String bucket = location.getS3Bucket();

			//uploadSource(amazonS3Client, bucket, AWS_S3_KEY);
			File file = new File("C:\\Users\\sahil.lone\\Desktop\\AWS-Release\\DUMMY_RELEASE.war");
			PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, AWS_S3_KEY, file);

			amazonS3Client.putObject(putObjectRequest);
			S3Location sourceBundle = new S3Location(bucket, AWS_S3_KEY);

			//createApplicationVersion based on Release No

			DescribeApplicationVersionsRequest describeApplicationVersionsRequest = new DescribeApplicationVersionsRequest()
					.withApplicationName(AWS_APPLICATION_NAME).withVersionLabels(APPLICATION_VERSION);
			Future<DescribeApplicationVersionsResult> describeApplicationVersionsResult = ebsClient
					.describeApplicationVersionsAsync(describeApplicationVersionsRequest);
			while (!describeApplicationVersionsResult.isDone())
			{
				Thread.sleep(sleepTimeForOperationsinMilliSeconds);
			}
			if (describeApplicationVersionsResult.get().getApplicationVersions().size() > 0)
			{
				DeleteApplicationVersionRequest deleteApplicationVersionRequest = new DeleteApplicationVersionRequest()
						.withApplicationName(AWS_APPLICATION_NAME).withVersionLabel(APPLICATION_VERSION);
				Future<DeleteApplicationVersionResult> deleteApplicationVersionResult = ebsClient
						.deleteApplicationVersionAsync(deleteApplicationVersionRequest);
				while (!deleteApplicationVersionResult.isDone())
				{
					Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				}

			}

			CreateApplicationVersionRequest applicationVersionRequest = new CreateApplicationVersionRequest()
					.withApplicationName(AWS_APPLICATION_NAME).withAutoCreateApplication(false)
					.withDescription(AWS_APPLICATION_DESCRIPTION).withProcess(false)
					.withVersionLabel(APPLICATION_VERSION).withSourceBundle(sourceBundle);
			Future<CreateApplicationVersionResult> createApplicationVersionResult = ebsClient
					.createApplicationVersionAsync(applicationVersionRequest);
			while (!createApplicationVersionResult.isDone())
			{
				Thread.sleep(sleepTimeForOperationsinMilliSeconds);
			}
			ApplicationVersionDescription applicationVersionDescription = createApplicationVersionResult.get()
					.getApplicationVersion();
			deploymentResult.setApplicationVersionDescription(applicationVersionDescription);
			if (null == applicationVersionDescription
					|| StringUtils.equalsIgnoreCase(applicationVersionDescription.getStatus(),
							ApplicationVersionStatus.Failed.toString()))
			{
				throw new EprocAWSException("Failed to create version for the current application");
			}

			//Create application environment based on application version
			DescribeEnvironmentsRequest describeEnvironmentsRequest = new DescribeEnvironmentsRequest()
					.withApplicationName(AWS_APPLICATION_NAME).withEnvironmentNames(AWS_APPLICATION_ENVIRONMENT_NAME)
					.withVersionLabel(APPLICATION_VERSION);
			Future<DescribeEnvironmentsResult> describeEnvironmentsResult = ebsClient
					.describeEnvironmentsAsync(describeEnvironmentsRequest);
			while (!describeEnvironmentsResult.isDone())
			{
				Thread.sleep(sleepTimeForOperationsinMilliSeconds);
			}
			String environmentId = "";
			if (describeEnvironmentsResult.get().getEnvironments().size() < 1)
			{

				EnvironmentTier environmentTier = new EnvironmentTier().withName("WebServer").withType("Standard");
				CreateEnvironmentRequest createEnvironmentRequest = new CreateEnvironmentRequest()
						.withApplicationName(AWS_APPLICATION_NAME).withCNAMEPrefix(CNAME_PREFIX)
						.withEnvironmentName(AWS_APPLICATION_ENVIRONMENT_NAME)
						.withSolutionStackName(AWS_SOLUTION_STACK).withVersionLabel(APPLICATION_VERSION)
						.withDescription(AWS_ENVIRONMENT_DESCRIPTION).withTier(environmentTier)
						.withDescription(AWS_APPLICATION_ENVIRONMENT_NAME);
				Future<CreateEnvironmentResult> createEnvironmentResult = ebsClient
						.createEnvironmentAsync(createEnvironmentRequest);
				while (!createEnvironmentResult.isDone())
				{
					Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				}
				deploymentResult.setCreateEnvironmentResult(createEnvironmentResult.get());

			}
			Iterator<EventDescription> environmentHealthEvents = null;
			String environmentStatus = null;
			boolean error = false;
			long startTime = System.currentTimeMillis();
			while (true)
			{

				describeEnvironmentsResult = ebsClient.describeEnvironmentsAsync(describeEnvironmentsRequest);
				while (!describeEnvironmentsResult.isDone())
				{
					Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				}
				if (describeEnvironmentsResult.get().getEnvironments().size() < 1)
				{
					error = true;
					break;
				}

				DescribeEventsRequest describeEventsRequest = new DescribeEventsRequest().withApplicationName(
						AWS_APPLICATION_NAME).withEnvironmentName(AWS_APPLICATION_ENVIRONMENT_NAME);
				Future<DescribeEventsResult> describeEventsResult = ebsClient
						.describeEventsAsync(describeEventsRequest);
				while (!describeEventsResult.isDone())
				{
					Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				}
				List<EventDescription> eventDescriptions = describeEventsResult.get().getEvents();
				environmentHealthEvents = eventDescriptions.iterator();
				deploymentResult.setEventDescriptions(eventDescriptions);
				EventDescription eventDescription = null;
				while (environmentHealthEvents.hasNext())
				{
					eventDescription = environmentHealthEvents.next();
					if (StringUtils.equals(eventDescription.getSeverity(), EventSeverity.ERROR.toString())
							|| StringUtils.equals(eventDescription.getSeverity(), EventSeverity.FATAL.toString())
							|| StringUtils.equals(eventDescription.getSeverity(), EventSeverity.WARN.toString()))
					{
						error = true;
					}
					else
					{
						environmentHealthEvents.remove();
					}
				}
				if (error == true)
				{
					deploymentResult.setEventDescriptions(eventDescriptions);
					break;
				}
				DescribeEnvironmentHealthRequest describeEnvironmentHealthRequest = new DescribeEnvironmentHealthRequest()
						.withAttributeNames(EnvironmentHealthAttribute.All)
						.withEnvironmentName(AWS_APPLICATION_ENVIRONMENT_NAME).withEnvironmentId(environmentId);

				Future<DescribeEnvironmentHealthResult> describeEnvironmentHealthResult = ebsClient
						.describeEnvironmentHealthAsync(describeEnvironmentHealthRequest);
				while (!describeEnvironmentHealthResult.isDone())
				{
					Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				}

				environmentStatus = describeEnvironmentHealthResult.get().getStatus();
				deploymentResult.setEnvironmentStatus(environmentStatus);
				if (environmentStatus == EnvironmentStatus.Terminated.toString()
						|| environmentStatus == EnvironmentStatus.Terminating.toString())
				{
					error = true;
					break;
				}
				else if (environmentStatus == EnvironmentStatus.Ready.toString())
				{
					break;
				}
				String healthStatus = describeEnvironmentHealthResult.get().getHealthStatus();
				deploymentResult.setEnvironmentHealthStatus(healthStatus);
				if (healthStatus == EnvironmentHealthStatus.Ok.toString()
						|| healthStatus == EnvironmentHealthStatus.Info.toString())
				{
					break;
				}
				else if (healthStatus == EnvironmentHealthStatus.Warning.toString()
						|| healthStatus == EnvironmentHealthStatus.Degraded.toString()
						|| healthStatus == EnvironmentHealthStatus.Severe.toString())
				{
					error = true;
					break;
				}
				Thread.sleep(sleepTimeForOperationsinMilliSeconds);
				if ((System.currentTimeMillis() - startTime) / 1000 > MAX_STATUS_UPDATE_TIME)
				{
					error = true;
					deploymentResult.setMaxHealthCheckFailed(true);
					break;
				}
			}
			if (error)
			{
				TerminateEnvironmentRequest terminateEnvironmentRequest = new TerminateEnvironmentRequest()
						.withEnvironmentId(environmentId).withEnvironmentName(AWS_APPLICATION_ENVIRONMENT_NAME)
						.withForceTerminate(true);
				ebsClient.terminateEnvironment(terminateEnvironmentRequest);
				throw new EprocAWSException(
						"Deployment failed . Please look into the deployment results for the reasons.");
			}
			else
			{
				deploymentResult.setDeploymentResult(DeploymentResult.DEPLOYMENT_RESULT_PASSED);
			}

			//get logs for determining the environment is launched or not

			//Get Health of the environment

			/*DescribeEnvironmentHealthRequest describeEnvironmentHealthRequest = new DescribeEnvironmentHealthRequest()
					.withAttributeNames("All").withEnvironmentName(AWS_APPLICATION_ENVIRONMENT_NAME);
			DescribeEnvironmentHealthResult describeEnvironmentHealthResult = ebsClient
					.describeEnvironmentHealth(describeEnvironmentHealthRequest);
			*/
			//swap Environment Cnames
			/*SwapEnvironmentCNAMEsRequest swapEnvironmentCNAMEsRequest = new SwapEnvironmentCNAMEsRequest().withDestinationEnvironmentName(
					"my-env-green").withSourceEnvironmentName("my-env-blue");
			SwapEnvironmentCNAMEsResult swapEnvironmentCNAMEsResult = ebsClient.swapEnvironmentCNAMEs(swapEnvironmentCNAMEsRequest);*/
		}
		catch (EprocAWSException e)
		{
			deploymentResult.setErrorMessage(e.getErrorMessage());
			deploymentResult.setErrorCode(e.getErrorCode());
			deploymentResult.setErrorType(AmazonServiceException.ErrorType.Client.toString());
			deploymentResult.setDeploymentResult(DeploymentResult.DEPLOYMENT_RESULT_FAILED);
		}
		catch (AmazonServiceException e)
		{
			deploymentResult.setErrorMessage(e.getErrorMessage());
			deploymentResult.setErrorCode(e.getErrorCode());
			deploymentResult.setErrorType(AmazonServiceException.ErrorType.Client.toString());
			deploymentResult.setDeploymentResult(DeploymentResult.DEPLOYMENT_RESULT_FAILED);
			deploymentResult.setStackTrace(ExceptionUtils.getStackTrace(e));

		}
		catch (Exception e)
		{
			deploymentResult.setErrorMessage(e.getMessage());
			deploymentResult.setErrorCode(e.getMessage());
			deploymentResult.setErrorType(AmazonServiceException.ErrorType.Client.toString());
			deploymentResult.setDeploymentResult(DeploymentResult.DEPLOYMENT_RESULT_FAILED);
			deploymentResult.setStackTrace(ExceptionUtils.getFullStackTrace(e));

		}

		return deploymentResult;

	}

	public static void InvokeLambdaFunctionAsync(BasicAWSCredentials awsCredentials)
	{
		String function_name = "HelloFunction";
		String function_input = "{\"who\":\"AWS SDK for Java\"}";

		AWSLambdaAsyncClient aws_lambda = new AWSLambdaAsyncClient();
		InvokeRequest req = new InvokeRequest().withFunctionName(function_name).withPayload(
				ByteBuffer.wrap(function_input.getBytes()));

		Future<InvokeResult> future_res = aws_lambda.invokeAsync(req);

		System.out.print("Waiting for future");
		while (future_res.isDone() == false)
		{
			System.out.print(".");
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				System.err.println("\nThread.sleep() was interrupted!");
				System.exit(1);
			}
		}

		try
		{
			InvokeResult res = future_res.get();
			if (res.getStatusCode() == 200)
			{
				System.out.println("\nLambda function returned:");
				ByteBuffer response_payload = res.getPayload();
				System.out.println(new String(response_payload.array()));
			}
			else
			{
				System.out.format("Received a non-OK response from AWS: %d\n", res.getStatusCode());
			}
		}
		catch (InterruptedException | ExecutionException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}

		System.exit(0);
	}

	public static class AsyncLambdaHandler implements AsyncHandler<InvokeRequest, InvokeResult>
	{

		public AsyncLambdaHandler(BasicAWSCredentials awsCredentials)
		{
			super();
		}

		public void onSuccess(InvokeRequest req, InvokeResult res)
		{
			System.out.println("\nLambda function returned:");
			ByteBuffer response_payload = res.getPayload();
			System.out.println(new String(response_payload.array()));
			System.exit(0);
		}

		public void onError(Exception e)
		{
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void InvokeLambdaFunctionAsyncWIthCallback(BasicAWSCredentials awsCredentials)
	{
		String function_name = "HelloFunction";
		String function_input = "{\"who\":\"AWS SDK for Java\"}";

		AWSLambdaAsyncClient aws_lambda = new AWSLambdaAsyncClient(awsCredentials);
		InvokeRequest req = new InvokeRequest().withFunctionName(function_name).withPayload(
				ByteBuffer.wrap(function_input.getBytes()));
		Future<InvokeResult> future_res = aws_lambda.invokeAsync(req, new AsyncLambdaHandler(awsCredentials));

		System.out.print("Waiting for async callback");
		while (!future_res.isDone() && !future_res.isCancelled())
		{
			// perform some other tasks...
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				System.err.println("Thread.sleep() was interrupted!");
				System.exit(0);
			}
			System.out.print(".");
		}
	}

	public static String escapeAWSChars(String input)
	{
		input = input.replace("\\n", "\n\t\t");
		input = input.replace("\\r", "\r");
		input = input.replace("\\t", "\t\t\t\t");
		input = input.replace("\\\"", "\"");
		return input = input.replace("_", "-");
	}

	public static String getCurrentTimeStamp()
	{
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");//dd/MM/yyyy
		Date now = new Date();
		String strDate = sdfDate.format(now);
		return strDate;
	}

	private void uploadSource(AmazonS3Client amazonS3Client, String bucket, String awsS3Key)
	{

		//define source file
		/*ZipFile zipFile = null;
		try
		{
			zipFile = new ZipFile("C:\\Users\\sahil.lone\\Desktop\\AWS-Release\\app.zip");
		}
		catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Enumeration<? extends ZipEntry> entries = zipFile.entries();

		while (entries.hasMoreElements())
		{
			ZipEntry zipEntry = entries.nextElement();
			System.out.println(zipEntry.getName());
			if (!zipEntry.isDirectory())
			{
				PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, AWS_S3_KEY + "/" + zipEntry.getName(),
						file);

				amazonS3Client.putObject(putObjectRequest);
			}
		}
		*/
		File dir = new File("C:\\Users\\sahil.lone\\Desktop\\AWS-Release\\DUMMY_RELEASE");
		File[] directoryListing = dir.listFiles();
		if (directoryListing != null)
		{
			for (File childFile : directoryListing)
			{
				PutObjectRequest putObjectRequest = new PutObjectRequest(bucket,
						AWS_S3_KEY + "/" + childFile.getName(), childFile);

				amazonS3Client.putObject(putObjectRequest);
				if (childFile.getName().equals(".ebextensions.zip"))
				{
					AWS_SOURCE_BUNDLE = AWS_S3_KEY + "/" + childFile.getName();
				}
			}
		}
		else
		{
			// Handle the case where dir is not really a directory.
			// Checking dir.isDirectory() above would not be sufficient
			// to avoid race conditions with another process that deletes
			// directories.
		}
		/*try
		{
			zipFile.close();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
}
