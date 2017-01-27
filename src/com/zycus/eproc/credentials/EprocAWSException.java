package com.zycus.eproc.credentials;

import com.amazonaws.AmazonServiceException;

public class EprocAWSException extends AmazonServiceException
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -3979347550755145327L;

	public EprocAWSException(String errorMessage)
	{
		super(errorMessage);

	}

}
