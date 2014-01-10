package com.hliang.batch.eod;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;


import com.hliang.batch.common.HashUtils;
import com.hliang.batch.eod.model.ProductFeed;

public class ProductFeedStageItemProcessor implements ItemProcessor<ProductFeed, ProductFeed>{
	private static Logger log = Logger.getLogger(ProductFeedStageItemProcessor.class);
	
	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	@Override
	public ProductFeed process(ProductFeed arg0) throws Exception {
		String sql = "select * from BATCH_STAGING where BATCH_KEY = ?";
		String ob = null;
		try
		{
			String json = arg0.toJson();
			String key = HashUtils.hash(arg0.toKeyString());
			if(log.isDebugEnabled()) log.debug("key is"+ key);
			ob = (String)jdbcTemplate.queryForObject(sql, new StageItemRowMapper(), key); 
		}catch(Exception e)
		{
			log.error("query throw error."+e.toString());
		}
		if(StringUtils.isBlank(ob))
		{
			
			return arg0;
		}else
		{
			if(log.isDebugEnabled()) log.debug("%%%%%%%%%%%%%% ITEM FOUND!!!!!!!!!!!!");
			return null;
		}
	}
	
	class StageItemRowMapper implements RowMapper{

		@Override
		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getString("BATCH_KEY");
		}
		
	}
	

}
