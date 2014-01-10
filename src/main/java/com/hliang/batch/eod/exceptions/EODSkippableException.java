package com.hliang.batch.eod.exceptions;

public class EODSkippableException extends RuntimeException {

	private static final long serialVersionUID = 224235512341L;
	public EODSkippableException() {
		super();
    }

    public EODSkippableException(String message) {
    	super(message);
    }
  
    public EODSkippableException(String message, Throwable cause) {
        super(message, cause);
    }

    public EODSkippableException(Throwable cause) {
        super(cause);
    }
}
