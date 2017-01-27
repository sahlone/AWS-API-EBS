package com.zycus.eproc.credentials;

import java.util.List;

import com.amazonaws.services.elasticbeanstalk.model.ApplicationVersionDescription;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionResult;
import com.amazonaws.services.elasticbeanstalk.model.CreateEnvironmentResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeApplicationsResult;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentHealthResult;
import com.amazonaws.services.elasticbeanstalk.model.EventDescription;

public class DeploymentResult
{

	public static final String		DEPLOYMENT_RESULT_FAILED		= "Deployment Failed";
	public static final String		DEPLOYMENT_RESULT_PASSED		= "Deployment sucessfull";
	private String					errorMessage					= null;
	private String					errorCode						= null;
	private String					errorType						= null;
	private String					applicationName					= null;
	private String					environmentName					= null;
	private String					versionLabel					= null;
	private String					environmentDNSNamePrefix		= null;
	CreateApplicationResult			createApplicationResult			= null;
	CreateEnvironmentResult			createEnvironmentResult			= null;
	List<EventDescription>			eventDescriptions					= null;
	DescribeEnvironmentHealthResult	describeEnvironmentHealthResult	= null;
	ApplicationVersionDescription	applicationVersionDescription	= null;
	private String					environmentStatus				= null;
	private String					environmentHealthStatus			= null;
	private boolean					maxHealthCheckFailed			= false;
	private String					deploymentResult				= DEPLOYMENT_RESULT_FAILED;
	private String					stackTrace						= null;

	public ApplicationVersionDescription getApplicationVersionDescription()
	{
		return applicationVersionDescription;
	}

	public void setApplicationVersionDescription(ApplicationVersionDescription applicationVersionDescription)
	{
		this.applicationVersionDescription = applicationVersionDescription;
	}

	public List<EventDescription> getEventDescipions()
	{
		return eventDescriptions;
	}

	public void setEventDescriptions(List<EventDescription> eventDescipions)
	{
		this.eventDescriptions = eventDescipions;
	}

	public String getErrorCode()
	{
		return errorCode;
	}

	public void setErrorCode(String errorCode)
	{
		this.errorCode = errorCode;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	public CreateApplicationResult getCreateApplicationResult()
	{
		return createApplicationResult;
	}

	public void setCreateApplicationResult(CreateApplicationResult createApplicationResult)
	{
		this.createApplicationResult = createApplicationResult;
	}

	public String getApplicationName()
	{
		return applicationName;
	}

	public void setApplicationName(String applicationName)
	{
		this.applicationName = applicationName;
	}

	public String getEnvironmentName()
	{
		return environmentName;
	}

	public void setEnvironmentName(String environmentName)
	{
		this.environmentName = environmentName;
	}

	public String getVersionLabel()
	{
		return versionLabel;
	}

	public void setVersionLabel(String versionLabel)
	{
		this.versionLabel = versionLabel;
	}

	public CreateEnvironmentResult getCreateEnvironmentResult()
	{
		return createEnvironmentResult;
	}

	public void setCreateEnvironmentResult(CreateEnvironmentResult createEnvironmentResult)
	{
		this.createEnvironmentResult = createEnvironmentResult;
	}

	public DescribeEnvironmentHealthResult getDescribeEnvironmentHealthResult()
	{
		return describeEnvironmentHealthResult;
	}

	public void setDescribeEnvironmentHealthResult(DescribeEnvironmentHealthResult describeEnvironmentHealthResult)
	{
		this.describeEnvironmentHealthResult = describeEnvironmentHealthResult;
	}

	public void setEventDescipions(List<EventDescription> eventDescipions)
	{
		this.eventDescriptions = eventDescipions;
	}

	public String getEnvironmentDNSNamePrefix()
	{
		return environmentDNSNamePrefix;
	}

	public void setEnvironmentDNSNamePrefix(String environmentDNSNamePrefix)
	{
		this.environmentDNSNamePrefix = environmentDNSNamePrefix;
	}

	public String getEnvironmentStatus()
	{
		return environmentStatus;
	}

	public void setEnvironmentStatus(String environmentStatus)
	{
		this.environmentStatus = environmentStatus;
	}

	public String getEnvironmentHealthStatus()
	{
		return environmentHealthStatus;
	}

	public void setEnvironmentHealthStatus(String environmentHealthStatus)
	{
		this.environmentHealthStatus = environmentHealthStatus;
	}

	public boolean isMaxHealthCheckFailed()
	{
		return maxHealthCheckFailed;
	}

	public void setMaxHealthCheckFailed(boolean maxHealthCheckFailed)
	{
		this.maxHealthCheckFailed = maxHealthCheckFailed;
	}

	public String getDeploymentResult()
	{
		return deploymentResult;
	}

	public void setDeploymentResult(String deploymentResult)
	{
		this.deploymentResult = deploymentResult;
	}

	public String getStackTrace()
	{
		return stackTrace;
	}

	public void setStackTrace(String stackTrace)
	{
		this.stackTrace = stackTrace;
	}

	public String getErrorType()
	{
		return errorType;
	}

	public void setErrorType(String errorType)
	{
		this.errorType = errorType;
	}

}
