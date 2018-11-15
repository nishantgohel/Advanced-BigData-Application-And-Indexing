package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@SpringBootApplication
public class RedisApplication {

	
	
	@Bean
	public ShallowEtagHeaderFilter shallowEtagFilter()
	{
		return new ShallowEtagHeaderFilter();
	}
	
	public static void main(String[] args) {
		SpringApplication.run(RedisApplication.class, args);
	}
}
