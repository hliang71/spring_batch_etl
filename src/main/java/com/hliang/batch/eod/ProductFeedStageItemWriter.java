package com.hliang.batch.eod;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

import com.hliang.batch.common.HashUtils;
import com.hliang.batch.eod.exceptions.EODSkippableException;
import com.hliang.batch.eod.exceptions.POFRetryableException;
import com.hliang.batch.eod.model.ProductFeed;


/**
 * Database {@link ItemWriter} implementing the process indicator pattern.
 */
public class ProductFeedStageItemWriter implements StepExecutionListener, ItemWriter<ProductFeed> {
    private static Logger log = Logger.getLogger(ProductFeedStageItemWriter.class);
    private static final String xmlDateFormat = "yyyy-MM-dd'T'HH:mm:ss";//"yyyy-MM-dd";
   
	public static final String NEW = "N";

	public static final String DONE = "Y";

	public static final Object WORKING = "W";

	private DataFieldMaxValueIncrementer incrementer;

	private StepExecution stepExecution;
	
	private JdbcTemplate simpleJdbcTemplate;

	private static final String BATCH_UPDATE_SQL="INSERT into BATCH_STAGING (ID, JOB_ID, VALUE, DATE, PROCESSED, BATCH_KEY, VERSION) values (?,?,?,?,?,?,?)";

	/**
	 * Setter for the key generator for the staging table.
	 * 
	 * @param incrementer the {@link DataFieldMaxValueIncrementer} to set
	 */
	public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
		this.incrementer = incrementer;
	}

	/**
	 * Serialize the item to the staging table, and add a NEW processed flag.
	 * 
	 * @see ItemWriter#write(java.util.List)
	 */
	public void write(final List<? extends ProductFeed> items) {

		final ListIterator<? extends ProductFeed> itemIterator = items.listIterator();
		JdbcTemplate sTemplate = this.getSimpleJdbcTemplate();
		
		List<Object[]> params = new ArrayList<Object[]>();
		
		long jobId = stepExecution.getJobExecution().getJobId();
		String key = null;
		ByteArrayOutputStream bos = null;
		try
		{
			while(itemIterator.hasNext())
			{
				try
				{
					Date today = new Date();
					SimpleDateFormat format = new SimpleDateFormat(xmlDateFormat);
					String todayStr = format.format(today);
					long id = incrementer.nextLongValue();
					bos = new ByteArrayOutputStream();
					ProductFeed bf = itemIterator.next();
					String json = bf.toJson();		
					key = HashUtils.hash(bf.toKeyString());
					byte[] blob = json.getBytes();
					Object[] param = new Object[]{id, jobId, blob, today, NEW, key, 1};
					params.add(param);
				}catch(Exception e)
				{
					log.error("Failed write to staging table. skipped item is ( key = " + key+" jobid = "+jobId+" )", e);

					throw new EODSkippableException(e);
				}
			}
			sTemplate.batchUpdate(BATCH_UPDATE_SQL, params);
			
		}catch(EODSkippableException e)
		{
						throw e;
		}catch(POFRetryableException ee)
		{
			throw ee;
		}catch(DeadlockLoserDataAccessException eee)
		{
			throw eee;
		}
		catch(Exception e)
		{	
			if(!(e instanceof DuplicateKeyException))
			{
				log.error("Failed to batch update xml to staging table. chrunk skipped", e);
				throw new EODSkippableException(e);
			}
		}finally
		{
			if(bos != null)
			{
				try
				{
					bos.close();
				}catch(IOException e)
				{
					if(log.isDebugEnabled()) log.debug("close stream failed.");
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.domain.StepListener#afterStep(StepExecution
	 * )
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.springframework.batch.core.domain.StepListener#beforeStep(org.
	 * springframework.batch.core.domain.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.domain.StepListener#onErrorInStep(java
	 * .lang.Throwable)
	 */
	public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable e) {
		return null;
	}

	
	
	public void setDataSource(DataSource dataSource) {
        this.simpleJdbcTemplate = new JdbcTemplate(dataSource);
    }

	public JdbcTemplate getSimpleJdbcTemplate() {
		return simpleJdbcTemplate;
	}
	
	
}
