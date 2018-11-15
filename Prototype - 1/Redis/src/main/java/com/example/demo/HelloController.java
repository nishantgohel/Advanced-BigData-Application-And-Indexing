package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.expression.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.jayway.jsonpath.PathNotFoundException;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


@RestController
public class HelloController
{
	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	
	
	//the jedis connection pool..
	private static JedisPool pool = null;
	
	public HelloController() 
	{
	    //configure our pool connection
	    pool = new JedisPool(redisHost, redisPort);

	}
	
	
	@RequestMapping("/")
    public String index()
	{
        return "Crisis on Infinite earth!";
        
    }
	
	
	//Setting the Schema in Redis
	@RequestMapping(value="/Plan/schema",method = RequestMethod.POST, consumes = "application/json")
	public ResponseEntity storeSchema(@RequestHeader HttpHeaders headers, @RequestBody String entity) throws ParseException, IOException
	{
		Jedis jedis = new Jedis("127.0.0.1", 6379);
		
		if(entity==null) 
		{
			  return new ResponseEntity("No Schema received!",HttpStatus.BAD_REQUEST) ;		 
		}
		
		String schemaFile = entity;
		jedis.set("Plan_schema", schemaFile);
		System.out.println(schemaFile);
		HttpHeaders httpHeaders = new HttpHeaders();
		return new ResponseEntity("Schema is posted successfully!", httpHeaders,HttpStatus.CREATED);
	}
	
	
	
	@RequestMapping(value="/Plan",method=RequestMethod.POST, consumes = "application/json")
	public ResponseEntity addPlan( @RequestHeader HttpHeaders headers,@RequestBody String jsonFile) throws IOException, URISyntaxException, ProcessingException, JSONException
	{
		System.out.println(jsonFile);
		
		Jedis jedis = pool.getResource();
		String schemaFile= jedis.get("Plan_schema");
		
		System.out.println(schemaFile);
		
		
		
		if (ValidationUtils.isJsonValid(schemaFile.toString(), jsonFile))
		{
			UUID idOne = UUID.randomUUID();
			String redisKey="Plan-"+idOne;
			System.out.println(redisKey);
			jedis.set(redisKey,jsonFile);
			
					
			System.out.println("Valid!");
			jedis.close();
			return new ResponseEntity("Data Validation Successful! and New plan is created with ID: "+redisKey, HttpStatus.CREATED);
		}
		else
		{
			
			System.out.println("NOT valid!");
			jedis.close();
			return new ResponseEntity("Data Not Valid", HttpStatus.BAD_REQUEST);
		}
		
		
	
	}
	
	
	
	@RequestMapping(value="/Plan/{planId}",method=RequestMethod.GET)
	public ResponseEntity getPlan(@PathVariable String planId)
	{
		Jedis jedis = pool.getResource();
		String result = jedis.get(planId);
		
		if(result==null)
		{
			jedis.close();
			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
		}
		else
		{
			jedis.close();
			return new ResponseEntity(result, HttpStatus.ACCEPTED);
		}
		
	}
	
	
	@RequestMapping(value="/Plan/{planId}",method=RequestMethod.DELETE)
	public ResponseEntity deletePlan(@PathVariable String planId)
	{
		Jedis jedis = pool.getResource();
		String result = jedis.get(planId);
		if(result==null)
		{
			jedis.close();
			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
		}
		else
		{
			jedis.del(planId);
			jedis.close();
			return new ResponseEntity("Data Deleted Successfully", HttpStatus.ACCEPTED);
		}
		
	}
	
	
	
	@RequestMapping(value="/Plan/{planId}",method=RequestMethod.PUT)
	public ResponseEntity updatePlan(@PathVariable String planId, @RequestBody String jsonFile) throws JSONException, ProcessingException, IOException
	{
		
		Jedis jedis = pool.getResource();
		String schemaFile= jedis.get("Plan_schema");
		
		System.out.println(schemaFile);
		
		
		if (ValidationUtils.isJsonValid(schemaFile, jsonFile))
		{
			String id = planId;
			String result = jedis.get(planId);
			
			if(result==null)
			{
				jedis.close();
				return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
			}
			else
			{
				System.out.println(jsonFile);
				jedis.del(planId);
				jedis.set(id,jsonFile);
				jedis.close();
				return new ResponseEntity("Data Updated Successfully", HttpStatus.ACCEPTED);
			}
		}
		else
		{
			System.out.println("NOT valid!");
			jedis.close();
			return new ResponseEntity("Data Not Valid", HttpStatus.BAD_REQUEST);
		}
		
	}
	
	
	
	
	
}
