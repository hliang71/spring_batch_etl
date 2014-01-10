
package com.hliang.eod.quartz;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;


import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.quartz.QuartzJobBean;


public class JobLauncherDetails extends QuartzJobBean {

	/**
	 * Special key in job data map for the name of a job to run.
	 */
	static final String JOB_NAME = "jobName";
	static final String CAN_RUN= "canRun";
	static final String NEXT_FIRE_TIME = "nextFireTime_mt";
	static final String DATE_FORMAT = "yyyy-MM-dd";
	static final String INPUT_FILE="inpuFile";

	private static Logger log = Logger.getLogger(JobLauncherDetails.class);

	private JobLocator jobLocator;

	private JobLauncher jobLauncher;

	/**
	 * Public setter for the {@link JobLocator}.
	 * @param jobLocator the {@link JobLocator} to set
	 */
	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	/**
	 * Public setter for the {@link JobLauncher}.
	 * @param jobLauncher the {@link JobLauncher} to set
	 */
	public void setJobLauncher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	@SuppressWarnings("unchecked")
	protected void executeInternal(JobExecutionContext context) {
		Map<String, Object> jobDataMap = context.getMergedJobDataMap();
		Boolean canRun =Boolean.valueOf( (String)jobDataMap.get(CAN_RUN));
		if(canRun)
		{
			Date nextFireTime = context.getNextFireTime();
			Long nextTimeStamp = nextFireTime.getTime();
			jobDataMap.put(NEXT_FIRE_TIME, nextTimeStamp);
			
			String jobName = (String) jobDataMap.get(JOB_NAME);
			log.info("Quartz trigger firing with Spring Batch jobName="+jobName+nextTimeStamp);
			JobParameters jobParameters = getJobParametersFromJobMap(jobDataMap);
			try {
				jobLauncher.run(jobLocator.getJob(jobName), jobParameters);
			}
			catch (JobExecutionException e) {
				log.error("Could not execute job.", e);
			}
		}else
		{
			log.info("EOD job is suspended in this environment, check the job configuration.");
		}
	}

	/*
	 * Copy parameters that are of the correct type over to
	 * {@link JobParameters}, ignoring jobName.
	 * 
	 * @return a {@link JobParameters} instance
	 */
	private JobParameters getJobParametersFromJobMap(Map<String, Object> jobDataMap) {

		JobParametersBuilder builder = new JobParametersBuilder();
        Long nextFireTime = (Long)jobDataMap.get(NEXT_FIRE_TIME);
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
       
        if(nextFireTime != null)
        {
        	String fireDate = format.format(new Date(nextFireTime));
        	String jobName = (String)jobDataMap.get(JOB_NAME);
        	StringBuilder sBuilder = new StringBuilder(jobName);
        	sBuilder.append("-");
        	sBuilder.append(fireDate);
        	sBuilder.append("-");
        	sBuilder.append(nextFireTime);
        	builder.addString(JOB_NAME, sBuilder.toString());
        	log.info("job param is "+sBuilder.toString());
        }
    	for (Entry<String, Object> entry : jobDataMap.entrySet()) 
    	{
			String key = entry.getKey();
			Object value = entry.getValue();
			log.info("key is "+key+" value is "+value);
			if(value instanceof String && key.equals(JOB_NAME)){
				builder.addString(key, (String) value);
			}
			else if (value instanceof String) {
				builder.addString(key, (String) value);
			}
			else if (value instanceof Float || value instanceof Double) {
				builder.addDouble(key, ((Number) value).doubleValue());
			}
			else if (value instanceof Integer || value instanceof Long) {
				builder.addLong(key, ((Number)value).longValue());
			}
			else if (value instanceof Date) {
				builder.addDate(key, (Date) value);
			}
			else {
				log.debug("JobDataMap contains values which are not job parameters (ignoring).");
			}
    	}
		
		

		return builder.toJobParameters();

	}

}
