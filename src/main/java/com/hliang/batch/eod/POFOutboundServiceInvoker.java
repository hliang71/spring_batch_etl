package com.hliang.batch.eod;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemWriter;

import com.hliang.batch.eod.exceptions.EODSkippableException;
import com.hliang.batch.eod.exceptions.POFRetryableException;
import com.hliang.batch.eod.model.ProductFeed;
import com.hliang.batch.service.POFOutboundServices;

public class POFOutboundServiceInvoker  implements ItemWriter<ProductFeed>{
	private static final Logger log = Logger.getLogger(POFOutboundServiceInvoker.class);
    private POFOutboundServices outboundServices;
	
    @Override
	public void write(List<? extends ProductFeed> arg0) throws Exception {
		try
		{
			for(ProductFeed pof : arg0)
			{
				outboundServices.convertAndSend(pof);
			}	
			
		}catch(POFRetryableException e)
		{
			log.info("txn retried."+e.toString());
			throw e;
		}catch(EODSkippableException e)
		{
			log.info("txn skipped."+e.toString());
			throw e;
		}catch(Exception e)
		{
			log.error("unhandled exception skipped the item.", e);
			throw new EODSkippableException(e);
		}
		
	}
	public POFOutboundServices getOutboundServices() {
		return outboundServices;
	}
	public void setOutboundServices(POFOutboundServices outboundServices) {
		this.outboundServices = outboundServices;
	}

}
