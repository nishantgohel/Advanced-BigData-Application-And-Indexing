package com.example.demo;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@ComponentScan
@EnableWebMvc
public class WebConfig 
{
	
	public WebConfig() 
	{
		super();
	}
	
	@Bean
	public FilterRegistrationBean filterReg()
	{
		final FilterRegistrationBean reg =  new FilterRegistrationBean();
		reg.setFilter(etagFilter());
		reg.addUrlPatterns("/Plan/*");
		reg.setName("etagFilter");
		reg.setOrder(1);
		System.out.println("inside filterreg");
		return reg;
	}
	
	@Bean(name="etagFilter")
	public Filter etagFilter()
	{
		System.out.println("Etag Filter is Hit");
		return new ShallowEtagHeaderFilter();
	}

}
