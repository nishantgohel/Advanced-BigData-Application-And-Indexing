package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
@EnableAutoConfiguration
@ComponentScan("com.example")
public class BigdataclassprojectApplication
{

	@Bean
	public ShallowEtagHeaderFilter shallowEtagFilter()
	{
		return new ShallowEtagHeaderFilter();
	}
	
	
	public static void main(String[] args)
	{
		SpringApplication.run(BigdataclassprojectApplication.class, args);
	}
}
