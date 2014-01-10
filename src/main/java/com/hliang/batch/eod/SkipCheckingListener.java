package com.hliang.batch.eod;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.annotation.OnSkipInRead;
import org.springframework.batch.core.annotation.OnSkipInWrite;

import com.hliang.batch.eod.model.ProductFeed;



public class SkipCheckingListener {

	private static final Log logger = LogFactory.getLog(SkipCheckingListener.class);
	private static int processSkips;
	private List<String> skippedRecords = new CopyOnWriteArrayList<String>();
	private String resource;

	@AfterStep
	public ExitStatus checkForSkips(StepExecution stepExecution){
		FileWriter writer = null;
		try
		{
			if(!this.skippedRecords.isEmpty())
			{
				File dir = new File(this.getResource());
				if(!dir.exists())
				{
					dir.mkdirs();
				}
				File output = new File(dir, String.valueOf(new Date().getTime()));
				
				writer = new FileWriter(output); 
				for(String str: skippedRecords) {
				  writer.write(str+"\n");	  
				}
				this.skippedRecords.removeAll(this.skippedRecords);
			}
		}catch(Exception e)
		{
			logger.error(e);
		}finally
		{
			if(writer != null)
			{
				try
				{
					writer.close();
				}catch(Exception e)
				{
					logger.error(e);
				}
			}
		}
		
		if (!stepExecution.getExitStatus().getExitCode().equals(ExitStatus.FAILED.getExitCode())
				&& stepExecution.getSkipCount() > 0) {
			return new ExitStatus("COMPLETED WITH SKIPS");
		}
		else {
			return null;
		}
	}
	
	/**
	 * Convenience method for testing
	 * @return the processSkips
	 */
	public static int getProcessSkips() {
		return processSkips;
	}

	/**
	 * Convenience method for testing
	 */
	public static void resetProcessSkips() {
		processSkips = 0;
	}

	@OnSkipInRead
	public void skipRead(Throwable t) {
		logger.debug("Skipped read " + t.getMessage());
		this.skippedRecords.add(t.getMessage());
		
	}
	@OnSkipInWrite
	public void skipWrite(ProductFeed feed, Throwable t) {
		
		this.skippedRecords.add(feed.toString());
		
	}

	@OnSkipInProcess
	public void skipProcess(ProductFeed feed, Throwable t) {
		
		processSkips++;
	}

	@BeforeStep
	public void saveStepName(StepExecution stepExecution) {
		stepExecution.getExecutionContext().put("stepName", stepExecution.getStepName());
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}
	
}
