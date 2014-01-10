package com.hliang.batch.eod;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class InitEODConfiguration {
	static
	{
		@SuppressWarnings("unused")
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:quartz-job-launcher-context.xml");
	}

	public static final void main(String[] args)
	{
		
	}
}
