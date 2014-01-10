package com.hliang.batch.service;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import com.hliang.batch.eod.exceptions.EODSkippableException;
import com.hliang.batch.eod.exceptions.POFRetryableException;
import com.hliang.batch.eod.model.ProductFeed;

public class POFOutboundServicesImpl implements POFOutboundServices {
	private static final Logger log = Logger.getLogger(POFOutboundServicesImpl.class);
	
    
	@Override
	/**
	 * if the method throw pofretryableexception the tx will be retry, if the method throw eodskippableexception the record will be skipped, and
	 * uncaught exception throw from this method will failed the batch step 
	 * */
	public void convertAndSend(ProductFeed pof) throws POFRetryableException,
			EODSkippableException {
		FileWriter writer = null;
		try
		{
			
			File dir = new File("batch/outbound/");
			if(!dir.exists())
			{
				dir.mkdirs();
			}
			File output = new File(dir, String.valueOf("POF_OUT_"+new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
			
			writer = new FileWriter(output, true); 
			
			writer.write(pof.toString());
		}catch(Exception e)
		{
			log.error(e);
			throw new EODSkippableException(e);
		}finally
		{
			if(writer != null)
			{
				try
				{
					writer.close();
				}catch(Exception e)
				{
					log.error(e);
				}
			}
		}
		
	}

}
