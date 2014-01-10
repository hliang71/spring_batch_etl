package com.hliang.batch.common;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class ErrorLogTasklet implements Tasklet, StepExecutionListener {

	protected final Log logger = LogFactory.getLog(getClass());

	private String resource;

	private String jobName;

	private StepExecution stepExecution;

	private String stepName;

	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		Assert.notNull(this.stepName, "Step name not set.  Either this class was not registered as a listener "
				+ "or the key 'stepName' was not found in the Job's ExecutionContext.");
		FileWriter writer = null;
		String msg = getSkipCount()+" records were skipped! by')"+jobName+"."+stepName;
		
		try
		{
			File dir = new File(this.getResource());
			if(!dir.exists())
			{
				dir.mkdirs();
			}
			File output = new File(dir, String.valueOf(new Date().getTime()));
			
			writer = new FileWriter(output); 
			writer.write(msg+"\n");
			
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
		return RepeatStatus.FINISHED;
	}

	/**
	 * @return
	 */
	private int getSkipCount() {
		if (stepExecution == null || stepName == null) {
			return 0;
		}
		for (StepExecution execution : stepExecution.getJobExecution().getStepExecutions()) {
			if (execution.getStepName().equals(stepName)) {
				return execution.getSkipCount();
			}
		}
		return 0;
	}

	

	public void beforeStep(StepExecution stepExecution) {
		this.jobName = stepExecution.getJobExecution().getJobInstance().getJobName().trim();
		this.stepName = (String) stepExecution.getJobExecution().getExecutionContext().get("stepName");
		this.stepExecution = stepExecution;
		stepExecution.getJobExecution().getExecutionContext().remove("stepName");
	}

	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	public String getResource() {
		return resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}

}
