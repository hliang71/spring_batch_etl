package com.hliang.batch.eod.exceptions;

public class POFRetryableException extends RuntimeException {

	private static final long serialVersionUID = 224235512351L;
	public POFRetryableException() {
		super();
    }

    public POFRetryableException(String message) {
    	super(message);
    }
  
    public POFRetryableException(String message, Throwable cause) {
        super(message, cause);
    }

    public POFRetryableException(Throwable cause) {
        super(cause);
    }
	
}
