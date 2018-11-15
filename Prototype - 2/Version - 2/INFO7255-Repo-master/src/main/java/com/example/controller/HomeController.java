package com.example.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.everit.json.schema.Schema;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.beans.JedisBean;
import com.example.beans.MyJsonValidator;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import redis.clients.jedis.Jedis;

@RestController
public class HomeController {
	
	@Autowired
	private MyJsonValidator validator;
	@Autowired
	private JedisBean jedisBean;
	
	private String key = "abcdefghijklmnopqrstuvwx";
	private String algorithm = "DESede";
	private static String finalKey = "0123456789abcdef";
	Jedis jedis = new Jedis();
	
	@RequestMapping("/")
	public String home() {
		return "Crisis On Infinite Earth";
	}
	
	
	// Inserting Schema
	
	@PostMapping("/Plan/Schema")
	public ResponseEntity<String> insertSchema(@RequestHeader HttpHeaders headers, @RequestBody(required=true) String body) 
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
		
		boolean authorized = authorizeToken(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
				
		
		
		
		
		
		if(body == null || body.isEmpty())
		{
			return new ResponseEntity("No Schema received", HttpStatus.BAD_REQUEST);
		}
				
		// set json schema in redis
		if(!jedisBean.insertSchema(body))
		{
			return new ResponseEntity("Schema insertion failed", HttpStatus.BAD_REQUEST);
		}
		validator.refreshSchema();
		return new ResponseEntity("Schema posted successfully", HttpStatus.ACCEPTED);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	// GET Plan
	
	@GetMapping("/Plan/{id}")
	public ResponseEntity<String> read(@PathVariable(name="id", required=true) String id, @RequestHeader HttpHeaders headers) {
		
//		if(!authorize(requestHeaders)) {
//			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
//		}
		
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
		
		boolean authorized = authorizeToken(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		
		
		
		
		
		//HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		System.out.println("Reading");
		String jsonString = jedisBean.read(id);
		if(jsonString != null)
		{
			return new ResponseEntity<String>(jsonString, HttpStatus.ACCEPTED);
		}
		else
		{
			return new ResponseEntity<String>("Read Unsuccessfull", headers, HttpStatus.BAD_REQUEST);
		}
	}
	
	
	// POST Plan
	
	@PostMapping("/Plan")
	public ResponseEntity<String> insert(@RequestBody(required=true) String body, @RequestHeader HttpHeaders headers) throws ProcessingException, IOException {
		
//		if(!authorize(requestHeaders)) {
//			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
//		}
		
		
		
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
		
		boolean authorized = authorizeToken(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		
		
		
		
		
		
		if(body==null || body.isEmpty()) 
		{
			  return new ResponseEntity("No Schema received!",HttpStatus.BAD_REQUEST) ;		 
		}
		String schemaFile= jedis.get("plan_schema");
		System.out.println(schemaFile);
		boolean flag = ValidationUtils.isJsonValid(schemaFile.toString(), body);
		
		if(flag)
		{
			JSONObject jsonObject = new JSONObject(body);
			String uuid = jedisBean.insert(jsonObject);
			return new ResponseEntity("Data Validation Successful! and New plan is created with ID: " + uuid,HttpStatus.CREATED);
		}
		
		return new ResponseEntity("Data Not Valid", HttpStatus.BAD_REQUEST);
		
		
		
		
//		Schema schema = validator.getSchema();
//		if(schema == null)
//		{
//			return new ResponseEntity<String>("schema file not found exception", HttpStatus.BAD_REQUEST);
//		}
//		
//		JSONObject jsonObject = validator.getJsonObjectFromString(body);
//		
//		if(validator.validate(jsonObject)) 
//		{
//			String uuid = jedisBean.insert(jsonObject);
//			return new ResponseEntity<String>("Inserted with id "+uuid, HttpStatus.ACCEPTED);
//		}
//		else
//		{
//			return new ResponseEntity<String>("invalid", HttpStatus.BAD_REQUEST);
//		}
			
	}
	
	
	// DELETE Plan
	
	@DeleteMapping("/Plan/{id}")
	public ResponseEntity<String> delete(@PathVariable(name="id", required=true) String id, @RequestHeader HttpHeaders headers)
	{
		
//		if(!authorize(requestHeaders)) {
//			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
//		}
		
		
		
		
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
		
		boolean authorized = authorizeToken(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		
		
		
		
		
		
		String key = id;
		if(key == null || key.isEmpty())
		{
			return new ResponseEntity("Key is Empty or Null", HttpStatus.NOT_FOUND);
		}
		
		if(jedisBean.delete(id))
			return new ResponseEntity<String>(id+" deleted successfully", HttpStatus.ACCEPTED);
		else
			return new ResponseEntity<String>("Deletion unsuccessfull", HttpStatus.BAD_REQUEST);
	}
	
	
	// UPDATE Plan
	
	@PutMapping("/Plan")
	public ResponseEntity<String> update(@RequestBody(required=true) String body, @RequestHeader HttpHeaders headers) throws ProcessingException, IOException
	{
		
//		if(!authorize(headers)) {
//			System.out.println("Authorization passed");
//			return new ResponseEntity<String>("Token authorization failed", HttpStatus.NOT_ACCEPTABLE);
//		}
		
		
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
		
		boolean authorized = authorizeToken(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			return new ResponseEntity("Token is Expired or Invalid Token", HttpStatus.UNAUTHORIZED);
		}
		
		
		
		
		
		
		
		
		
		
		if(body==null || body.isEmpty()) 
		{
			  return new ResponseEntity("No Schema received!",HttpStatus.BAD_REQUEST) ;		 
		}
		
		String schemaFile= jedis.get("plan_schema");
		boolean flag = ValidationUtils.isJsonValid(schemaFile.toString(), body);
		
		if(flag)
		{
			JSONObject jsonObject = new JSONObject(body);
			if(jedisBean.update(jsonObject))
			{
				return new ResponseEntity<String>("JSON instance updated in redis", HttpStatus.ACCEPTED);
			}
		}
		
		
		return new ResponseEntity<String>("Failed to update JSON instance in Redis", HttpStatus.BAD_REQUEST);
		
		
		
		
		
		
		
//		Schema schema = validator.getSchema();
//		if(schema == null)
//			return new ResponseEntity<String>("schema file not found exception", HttpStatus.BAD_REQUEST);
//		
//		System.out.println("Schema retreived succesfully");
//		JSONObject jsonObject = validator.getJsonObjectFromString(body);
//		
//		if(!jedisBean.update(jsonObject))
//			return new ResponseEntity<String>("Failed to update JSON instance in Redis", HttpStatus.BAD_REQUEST);
//		
//		System.out.println("");
		//return new ResponseEntity<String>("JSON instance updated in redis", HttpStatus.ACCEPTED);
	
	}
	
	@GetMapping("/Token")
	public ResponseEntity<String> createToken() {
		
		JSONObject jsonToken = new JSONObject ();
		jsonToken.put("organization", "Northeastern");
		jsonToken.put("issuer", "Nishant");
		
		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());            
		calendar.add(Calendar.MINUTE, 20);
		Date date = calendar.getTime();		

		
		jsonToken.put("expiry", df.format(date));
		String token = jsonToken.toString();
		System.out.println(token);
		
		SecretKey spec = loadKey();
		
		try {
			Cipher c = Cipher.getInstance(algorithm);
			c.init(Cipher.ENCRYPT_MODE, spec);
			byte[] encrBytes = c.doFinal(token.getBytes());
			String encoded = Base64.getEncoder().encodeToString(encrBytes);
			return new ResponseEntity<String>(encoded, HttpStatus.ACCEPTED);
			
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<String>("Token creation failed", HttpStatus.NOT_ACCEPTABLE);
		}
		
	}
	
	private SecretKey loadKey() {
		return new SecretKeySpec(key.getBytes(), algorithm);
	}
	
	private boolean authorize(HttpHeaders headers) {
		
		String token = headers.getFirst("Authorization").substring(7);
		byte[] decrToken = Base64.getDecoder().decode(token);
		SecretKey spec = loadKey();
		try {
			Cipher c = Cipher.getInstance(algorithm);
			c.init(Cipher.DECRYPT_MODE, spec);
			String tokenString = new String(c.doFinal(decrToken));
			JSONObject jsonToken = new JSONObject(tokenString);
			System.out.println(tokenString);
			System.out.println("Inside authorize");
			System.out.println(jsonToken.toString());
			
			String ttldateAsString = jsonToken.get("expiry").toString();
			Date currentDate = Calendar.getInstance().getTime();
			
			TimeZone tz = TimeZone.getTimeZone("UTC");
			DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // Quoted "Z" to indicate UTC, no timezone offset
			formatter.setTimeZone(tz);
			
			Date ttlDate = formatter.parse(ttldateAsString);
			currentDate = formatter.parse(formatter.format(currentDate));
			
			if(currentDate.after(ttlDate)) {
				return false;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	
	
	
	
	/*
	 * MY TOKEN METHOOD
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
		calendar.add(Calendar.MINUTE, 10);
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
	
	
	
	public boolean authorizeToken(String token)
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
		
		boolean authorized = authorizeToken(token1);
		System.out.println("Authorized value is "+authorized);
		
		
		if(authorized == false)
		{
			System.out.println("Token is Expired or Invalid Token");
		}
	}
	
	
	
	
	
	
	
	
	
	
	


}
