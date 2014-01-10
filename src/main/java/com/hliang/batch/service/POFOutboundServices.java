package com.hliang.batch.service;

import com.hliang.batch.eod.exceptions.EODSkippableException;
import com.hliang.batch.eod.exceptions.POFRetryableException;
import com.hliang.batch.eod.model.ProductFeed;

public interface POFOutboundServices {
	
	void convertAndSend(ProductFeed pof) throws POFRetryableException, EODSkippableException;

}
