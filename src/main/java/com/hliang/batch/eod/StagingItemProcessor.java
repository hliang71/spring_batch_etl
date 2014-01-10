package com.hliang.batch.eod;

import javax.sql.DataSource;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import com.hliang.batch.common.ProcessIndicatorItemWrapper;
import com.hliang.batch.eod.exceptions.POFRetryableException;
import com.hliang.batch.eod.model.ProductFeed;

/**
 * Marks the input row as 'processed'. (This change will rollback if there is
 * problem later)
 * 
 * @param <T> item type
 * 
 * @see StagingItemReader
 * @see StagingItemWriter
 * @see ProcessIndicatorItemWrapper
 * 
 */
public class StagingItemProcessor implements ItemProcessor<ProcessIndicatorItemWrapper<ProductFeed>, ProductFeed>, InitializingBean {

	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "Either jdbcTemplate or dataSource must be set");
	}

	/**
	 * Use the technical identifier to mark the input row as processed and
	 * return unwrapped item.
	 */
	public ProductFeed process(ProcessIndicatorItemWrapper<ProductFeed> wrapper) throws Exception {
        try
        {
        	long version = wrapper.getId().getVersion();
        	long newVersion = version + 1;
        	int count = jdbcTemplate.update("UPDATE BATCH_STAGING SET PROCESSED=?, VERSION=? WHERE ID=? AND PROCESSED=? AND VERSION=?",
				ProductFeedStageItemWriter.DONE, newVersion, wrapper.getId().getId(), ProductFeedStageItemWriter.NEW, version);
			if (count != 1) {
				return null;
			}
			return wrapper.getItem();
        }catch(Exception e)
        {
        	throw new POFRetryableException("The staging record with ID=" + wrapper.getId()
					+ " was updated concurrently when trying to mark as complete (updated 0 records.", e);
        }
	}

}
