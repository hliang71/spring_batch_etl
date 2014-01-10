package com.hliang.batch.eod;


import org.springframework.batch.item.ItemProcessor;


import com.hliang.batch.eod.model.ProductFeed;

public class ProductFeedItemProcessor implements ItemProcessor<ProductFeed, ProductFeed>{	

    public ProductFeed process(ProductFeed feed) throws Exception {
    	
        return feed;
    }
	
}
