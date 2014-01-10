package com.hliang.batch.eod;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemWriter;


public class ProductFeedItemWriter implements ItemWriter<String> {

	private static final Log log = LogFactory.getLog(ProductFeedItemWriter.class);
	private String resource;

	
	/**
	 * @see ItemWriter#write(java.util.List)
	 */
	public void write(List<? extends String> data) throws Exception {
		log.info("Write record:");
		log.info(data);
		FileWriter writer = null;
		try
		{
			File dir = new File(this.getResource());
			if(!dir.exists())
			{
				dir.mkdirs();
			}
			File output = new File(dir, String.valueOf(new Date().getTime()));
			
			writer = new FileWriter(output); 
			for(String str: data) {
			  writer.write(str+"\n");	  
			}
		}catch(Exception e)
		{
			log.error(e);
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

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

}