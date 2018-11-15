package com.example.demo;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletResponse;

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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.google.gson.Gson;
import com.jayway.jsonpath.PathNotFoundException;


import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


@RestController
public class HelloController
{
	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	private static String finalKey = "0123456789abcdef";
	Jedis jedis = new Jedis();
	
	//the jedis connection pool..
	//private static JedisPool pool = null;
	
	public HelloController() 
	{
	    //configure our pool connection
	    //pool = new JedisPool(redisHost, redisPort);
		

	}
	
	
	@RequestMapping("/")
    public String index()
	{
        return "Crisis on Infinite earth!";
        
    }
	
	/*
	 * For Testing OAuth
	 */
	@RequestMapping(value = "/test",method = RequestMethod.POST)
	public void test(@RequestHeader HttpHeaders headers)
	{
		String token = headers.getFirst("Authorization");
		if(token==null || token.isEmpty())
		{
			System.out.println("No token Found");
		}
		
		String token1 = "";	
		if(!token.contains("Bearer "))
		{
			System.out.println("Improper Format of Token");
		}
			
		
		
		token1 = token.substring(7);
		System.out.println("token value is "+token1);
		
		boolean authorized = authorize(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			System.out.println("Token is Expired or Invalid Token");
		}
	}
	
	
	//Setting the Schema in Redis
	@RequestMapping(value="/Plan/schema",method = RequestMethod.POST, consumes = "application/json")
	public ResponseEntity storeSchema(@RequestHeader HttpHeaders headers, @RequestBody String entity) throws ParseException, IOException
	{
		
		
		String token = headers.getFirst("Authorization");
		if(token==null || token.isEmpty())
		{
			return new ResponseEntity("No Token found!", HttpStatus.UNAUTHORIZED);
		}
		
		String token1 = "";	
		if(!token.contains("Bearer "))
		{
			return new ResponseEntity("Improper Format of Token", HttpStatus.UNAUTHORIZED);
		}
			
		
		
		token1 = token.substring(7);
		System.out.println("token value is "+token1);
		
		boolean authorized = authorize(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		//Jedis jedis = new Jedis("127.0.0.1", 6379);
		//Jedis jedis = pool.getResource();
		
		
		
		if(entity==null || entity.isEmpty()) 
		{
			  return new ResponseEntity("No Schema received!",HttpStatus.BAD_REQUEST) ;		 
		}
		
		
		String schemaFile = entity;
		jedis.set("Plan_schema", schemaFile);
		System.out.println(schemaFile);
		HttpHeaders httpHeaders = new HttpHeaders();
		//jedis.close();
		return new ResponseEntity("Schema is posted successfully!", httpHeaders,HttpStatus.CREATED);
	}
	
	
	
	@RequestMapping(value="/Plan",method=RequestMethod.POST, consumes = "application/json")
	public ResponseEntity addPlan(@RequestBody String jsonFile, @RequestHeader HttpHeaders headers) throws IOException, URISyntaxException, ProcessingException, JSONException
	{
				
		String token = headers.getFirst("Authorization");
		if(token==null || token.isEmpty())
		{
			return new ResponseEntity("No Token found!", HttpStatus.UNAUTHORIZED);
		}
		
		String token1 = "";	
		if(!token.contains("Bearer "))
		{
			return new ResponseEntity("Improper Format of Token", HttpStatus.UNAUTHORIZED);
		}
			
		token1 = token.substring(7);
		System.out.println("token value is "+token1);
		
		boolean authorized = authorize(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		
		
		System.out.println(jsonFile);		
		//Jedis jedis = pool.getResource();
		String schemaFile= jedis.get("Plan_schema");
		System.out.println(schemaFile);
		
		boolean flag = ValidationUtils.isJsonValid(schemaFile.toString(), jsonFile);
		System.out.println(flag);
		
		if(flag)
		{
			System.out.println("Inside IF");
			String key = insertPlan(jsonFile, headers);
			
//			UUID idOne = UUID.randomUUID();
//			String redisKey="Plan-"+idOne;
//			System.out.println(redisKey);
//			jedis.set(redisKey,jsonFile);
//			
//					
//			System.out.println("Valid!");
//			jedis.close();
			
			/*
			JSONObject usecase = new JSONObject(jsonFile);
			String key = usecase.get("objectType").toString() + "__" + usecase.get("objectId").toString();
			//JSONParser parser = new JSONParser();
			Map<String, Object> hashresult = new ObjectMapper().readValue(usecase.toString(), HashMap.class);
			Map<String, String> primaryProperties = new HashMap<String, String>();
			
			
			for (Map.Entry<String, Object> entry : hashresult.entrySet())
			{
				if (entry.getValue().getClass().toString().equals("class java.util.LinkedHashMap"))
				{
					LinkedHashMap value = (LinkedHashMap) entry.getValue();
					String s = new Gson().toJson(value, Map.class);
					JSONObject jsonobj = new JSONObject(s);
					String relationKey = key + "__" + entry.getKey();
					String relationValue = jsonobj.get("objectType").toString() + "__" + jsonobj.get("objectId").toString();
					jedis.sadd(relationKey,relationValue);
					addPlan(jsonobj.toString(), headers);
					
				}
				else if (entry.getValue().getClass().toString().equals("class java.util.ArrayList"))
				{
					ArrayList<LinkedHashMap> list = (ArrayList<LinkedHashMap>) entry.getValue();
					String listKey = key;
					
					for (LinkedHashMap value : list)
					{
						String s = new Gson().toJson(value, Map.class);
						JSONObject jsonobj = new JSONObject(s);
						String relationKey = listKey + "__" + entry.getKey();
						String relationValue = jsonobj.get("objectType").toString() + "__" + jsonobj.get("objectId").toString();
						jedis.sadd(relationKey, relationValue);
						addPlan(jsonobj.toString(), headers);
					}
					
				}
				else
				{
					primaryProperties.put(entry.getKey(),entry.getValue().toString());
				}
				
				
			}
			
			jedis.hmset(key, primaryProperties);
			*/
			
			
			return new ResponseEntity("Data Validation Successful! and New plan is created with ID: " + key,HttpStatus.CREATED);
		}
		else
		{
			
			System.out.println("HALA MADRID");
			//jedis.close();
			return new ResponseEntity("Data Not Valid", HttpStatus.BAD_REQUEST);
		}
		
		
	
	}
	
	
	
	@RequestMapping(value="/Plan/{planId}",method=RequestMethod.GET)
	public ResponseEntity getPlan(@RequestHeader HttpHeaders headers, @PathVariable String planId)
	{
		
		String token = headers.getFirst("Authorization");
		if(token==null || token.isEmpty())
		{
			return new ResponseEntity("No Token found!", HttpStatus.UNAUTHORIZED);
			//return "No Token found";
			
		}
		
		String token1 = "";	
		if(!token.contains("Bearer "))
		{
			return new ResponseEntity("Improper Format of Token", HttpStatus.UNAUTHORIZED);
			//return "Improper Format of Token";
			
		}
		
		
		token1 = token.substring(7);
		System.out.println("token value is "+token1);
		
		boolean authorized = authorize(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
			//return "Token is Expired or Invalid Token";
			
		}
		
				
		
//		Jedis jedis = pool.getResource();
//		String result = jedis.get(planId);
//		if(result==null)
//		{
//			//jedis.close();
//			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
//		}
//		else
//		{
//			//jedis.close();
//			return new ResponseEntity(result, HttpStatus.ACCEPTED);
//		}
		
		jedis.connect();
		String key = planId;
		Set<String> plankey = jedis.keys(key);
		
				
		if (plankey.isEmpty())
		{
			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
			//return "Data Not Found "; 
			
		}
		else
		{
			Map<String, String> plan =  jedis.hgetAll(key);
			return new ResponseEntity(plan.toString(), HttpStatus.ACCEPTED);
			//return plan.toString();
				
		}
		
		
	}
	
	
	@RequestMapping(value="/Plan/{planId}",method=RequestMethod.DELETE)
	public ResponseEntity deletePlan(@RequestHeader HttpHeaders headers, @PathVariable String planId)
	{
		
		String token = headers.getFirst("Authorization");
		if(token==null || token.isEmpty())
		{
			return new ResponseEntity("No Token found!", HttpStatus.UNAUTHORIZED);
		}
		
		String token1 = "";	
		if(!token.contains("Bearer "))
		{
			return new ResponseEntity("Improper Format of Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		token1 = token.substring(7);
		System.out.println("token value is "+token1);
		
		boolean authorized = authorize(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
				
		
		//Jedis jedis = pool.getResource();
//		String result = jedis.get(planId);
//		if(result==null)
//		{
//			//jedis.close();
//			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
//		}
//		else
//		{
//			jedis.del(planId);
//			jedis.close();
//			return new ResponseEntity("Data Deleted Successfully", HttpStatus.ACCEPTED);
//		}
		
		
		
		jedis.connect();
		String key = planId;
		if(key == null || key.isEmpty())
		{
			return new ResponseEntity("Key is Empty or Null", HttpStatus.NOT_FOUND);
		}
		
		Set<String> plankey = jedis.keys(key);
		if (plankey.isEmpty())
		{
			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
		}
		
		jedis.del(key);
		return new ResponseEntity("Data Deleted Successfully", HttpStatus.ACCEPTED);
		
	}
	
	
	
	@RequestMapping(value="/Plan/{planId}",method=RequestMethod.PUT)
	public ResponseEntity updatePlan(@RequestHeader HttpHeaders headers, @PathVariable String planId, @RequestBody String jsonFile) throws JSONException, ProcessingException, IOException
	{
		
		String token = headers.getFirst("Authorization");
		if(token==null || token.isEmpty())
		{
			return new ResponseEntity("No Token found!", HttpStatus.UNAUTHORIZED);
		}
		
		String token1 = "";	
		if(!token.contains("Bearer "))
		{
			return new ResponseEntity("Improper Format of Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		token1 = token.substring(7);
		System.out.println("token value is "+token1);
		
		boolean authorized = authorize(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
				
		
		
		//Jedis jedis = pool.getResource();
		
		
		//System.out.println(schemaFile);
		
		
//		if (ValidationUtils.isJsonValid(schemaFile, jsonFile))
//		{
//			String id = planId;
//			String result = jedis.get(planId);
//			
//			if(result==null)
//			{
//				//jedis.close();
//				return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
//			}
//			else
//			{
//				System.out.println(jsonFile);
//				jedis.del(planId);
//				jedis.set(id,jsonFile);
//				//jedis.close();
//				return new ResponseEntity("Data Updated Successfully", HttpStatus.ACCEPTED);
//			}
//		}
//		else
//		{
//			System.out.println("NOT valid!");
//			//jedis.close();
//			return new ResponseEntity("Data Not Valid", HttpStatus.BAD_REQUEST);
//		}
		
		
		String schemaFile= jedis.get("Plan_schema");
		
		if(ValidationUtils.isJsonValid(schemaFile, jsonFile))
		{
			JSONObject usecase = new JSONObject(jsonFile);
			Map<String, Object> hashresult = new ObjectMapper().readValue(usecase.toString(), HashMap.class);
			Map<String, String> primaryProperty = new HashMap<String, String>();
			
			for (Map.Entry<String, Object> entry : hashresult.entrySet())
			{
				primaryProperty.put(entry.getKey(), entry.getValue().toString());
			}
			
			jedis.hmset(planId,primaryProperty);
			
			return new ResponseEntity("Update Data is " + jedis.hgetAll(planId).toString(), HttpStatus.ACCEPTED);
		}
		else
		{
			System.out.println("NOT valid!");
			return new ResponseEntity("Data Not Valid", HttpStatus.BAD_REQUEST);
			
		}
		
	}
	
	
	/*
	 * Generating Token
	 */
	@RequestMapping(value="/getToken",method = RequestMethod.GET)
	public ResponseEntity getToken(@RequestHeader HttpHeaders headers) throws JSONException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException
	{
		
		String initVector = "RandomInitVector";
		
		//create token(Sample token)
		JSONObject object = new JSONObject();
		object.put("organization", "example.com");
		object.put("user", "nishant");
		
		
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 50);
		Date date =  calendar.getTime();
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		object.put("ttl", df.format(date));
		
		
		//Partial token created
		String token = object.toString();
		System.out.println("Token values is " + token);
		System.out.println("TTL is : " + object.get("ttl"));
		
		
		IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
		SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		
		
		//encrypting token
		byte[] encrypted = cipher.doFinal(token.getBytes()); 
		
		// encoded token (Base64 encoding)
		String finalToken = org.apache.tomcat.util.codec.binary.Base64.encodeBase64String(encrypted); 
		
		
		
		
		
		return new ResponseEntity(finalToken,HttpStatus.CREATED);
	}
	
	
	/*
	 * Validating Token 
	 */
	public boolean authorize(String token)
	{
		
		try
		{
			
			System.out.println("token coming in authorize"+token);
			String initVector = "RandomInitVector";
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(token));
			String entityDecoded = new String(original);
			
			
			System.out.println("*****Entity Decoded is "+entityDecoded);
			
			
			JSONObject object = new JSONObject(entityDecoded);
			Object arrayOfTests = object.get("ttl");
			Calendar calendar = Calendar.getInstance();
			Date date =  calendar.getTime();
			String getDate = arrayOfTests.toString();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			
			Date end = formatter.parse(getDate);
			Date start = formatter.parse(formatter.format(date));
			
			System.out.println(start.toString());
			System.out.println(end.toString());
			
			if(!start.before(end))
			{
				System.out.println("The Token Validity has expired");
				return false;	
			}
			
		}
		catch(Exception e)
		{
			System.out.println("inside exception---"+ e);
			return false;
		}
		
		return true;
	}
	
	public String insertPlan(String jsonFile, HttpHeaders headers) throws JSONException, JsonParseException, JsonMappingException, IOException
	{
		JSONObject usecase = new JSONObject(jsonFile);
		String key = usecase.get("objectType").toString() + "__" + usecase.get("objectId").toString();
		//JSONParser parser = new JSONParser();
		Map<String, Object> hashresult = new ObjectMapper().readValue(usecase.toString(), HashMap.class);
		Map<String, String> primaryProperties = new HashMap<String, String>();
		
		
		for (Map.Entry<String, Object> entry : hashresult.entrySet())
		{
			if (entry.getValue().getClass().toString().equals("class java.util.LinkedHashMap"))
			{
				LinkedHashMap value = (LinkedHashMap) entry.getValue();
				String s = new Gson().toJson(value, Map.class);
				JSONObject jsonobj = new JSONObject(s);
				String relationKey = key + "__" + entry.getKey();
				String relationValue = jsonobj.get("objectType").toString() + "__" + jsonobj.get("objectId").toString();
				jedis.sadd(relationKey,relationValue);
				insertPlan(jsonobj.toString(), headers);
				
			}
			else if (entry.getValue().getClass().toString().equals("class java.util.ArrayList"))
			{
				ArrayList<LinkedHashMap> list = (ArrayList<LinkedHashMap>) entry.getValue();
				String listKey = key;
				
				for (LinkedHashMap value : list)
				{
					String s = new Gson().toJson(value, Map.class);
					JSONObject jsonobj = new JSONObject(s);
					String relationKey = listKey + "__" + entry.getKey();
					String relationValue = jsonobj.get("objectType").toString() + "__" + jsonobj.get("objectId").toString();
					jedis.sadd(relationKey, relationValue);
					insertPlan(jsonobj.toString(), headers);
				}
				
			}
			else
			{
				primaryProperties.put(entry.getKey(),entry.getValue().toString());
			}
			
			
		}
		
		jedis.hmset(key, primaryProperties);
		return key;

	}
	
	
	
}
