package com.hliang.batch.eod;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;


import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;

import com.google.gson.Gson;
import com.hliang.batch.common.ProcessIndicatorItemWrapper;
import com.hliang.batch.eod.exceptions.POFRetryableException;
import com.hliang.batch.eod.model.ProductFeed;





/**
 * Thread-safe database {@link ItemReader} implementing the process indicator
 * pattern.
 * 
 * To achieve restartability use together with {@link StagingItemProcessor}.
 */
public class StagingItemReader implements ItemReader<ProcessIndicatorItemWrapper<ProductFeed>>, StepExecutionListener,
		InitializingBean, DisposableBean {

	private static Logger log = Logger.getLogger(StagingItemReader.class);

	private StepExecution stepExecution;

	private final Object lock = new Object();

	private volatile boolean initialized = false;

	private volatile Iterator<Key> keys;

	private JdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void destroy() throws Exception {
		initialized = false;
		keys = null;
	}

	public final void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "You must provide a DataSource.");
	}

	private List<Key> retrieveKeys() {

		synchronized (lock) {
			return jdbcTemplate.query(

			"SELECT ID, VERSION FROM BATCH_STAGING WHERE PROCESSED=? ORDER BY ID",

			new RowMapper<Key>() {
				public Key mapRow(ResultSet rs, int rowNum) throws SQLException {
					Key key = new Key();
					key.setId(rs.getLong(1));
					key.setVersion(rs.getLong(2));
					return key;
				}
			},

			ProductFeedStageItemWriter.NEW);

		}

	}
    public class Key 
    {
    	private long id;
    	private long version;
		public long getId() {
			return id;
		}
		public void setId(long id) {
			this.id = id;
		}
		public long getVersion() {
			return version;
		}
		public void setVersion(long version) {
			this.version = version;
		}
    }
	public ProcessIndicatorItemWrapper<ProductFeed> read() throws DataAccessException {
		
		if (!initialized) {
			throw new ReaderNotOpenException("Reader must be open before it can be used.");
		}
       
        Key id = null;
        ProcessIndicatorItemWrapper<ProductFeed> returnedResult = null;
    	
		synchronized (lock) {
			if (keys.hasNext()) {
				id = keys.next();
			}
		}
		if(log.isDebugEnabled())
		{
			log.debug("#################################################Retrieved key from list: ###########################################################" + id);			
		}

		if (id == null) {
			return null;
		}
		ProductFeed result =(ProductFeed)jdbcTemplate.queryForObject("SELECT VALUE FROM BATCH_STAGING WHERE ID=?",
				new RowMapper<Object>() {
					public Object mapRow(final ResultSet rs, final int rowNum) throws SQLException {
						InputStream pofIs = null;
						Reader pofReader = null;
						try{
    						final byte[] blob = rs.getBytes(1);						
    						if(blob == null)
    						{
    							return null;
    						}
    						pofIs = new ByteArrayInputStream(blob);
    						pofReader = new InputStreamReader(pofIs);
    						final Gson gson = new Gson();
    						final ProductFeed pf = gson.fromJson(pofReader, ProductFeed.class);
    						return pf;
						}
						finally
						{
							if(pofReader != null)
							{
								try
								{
									pofReader.close();
								}catch(final Exception e)
								{
									if(log.isDebugEnabled()) log.debug("close pof reader failed!");
								}
							}
							if(pofIs != null)
							{
								try
								{
									pofIs.close();
								}catch(final Exception e)
								{
									if(log.isDebugEnabled()) log.debug("close inputstream failed!");
								}
							}
						}
					}
				}, id.getId());
       
        if(result == null)
        {
        	throw new POFRetryableException("invalid staging data, going to retry");
        }
        returnedResult = new ProcessIndicatorItemWrapper<ProductFeed>(id, result);
        return returnedResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.domain.StepListener#afterStep(StepExecution
	 * )
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		
		//jdbcTemplate.update("ALTER TABLE `current_activity` ENABLE KEYS");
		
		synchronized (lock) 
		{
			keys = null;
		}
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
		synchronized (lock) {
			if (keys == null) {
				keys = retrieveKeys().iterator();
				log.info("Keys obtained for staging.");
				initialized = true;
			}
		}
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
}
