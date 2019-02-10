package com.example.demo.controller;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.ClientProtocolException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;

import redis.clients.jedis.Jedis;

@RestController
public class MainController {

	
	@Autowired
	private JedisBean jedisBean;
	
	@Autowired
	private ElasticSearchConnect elasticSearchConnect;
	
	Jedis jedis = new Jedis();
	private static String finalKey = "0123456789abcdef";
	
	public MainController() {}
	
	@RequestMapping("/")
	public String home() {
		return "Crisis On Infinite Earth..";
	}

	/***
	 * REST API to post schema plan
	 * 
	 * @param headers
	 * @param entity
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	@RequestMapping(value = "/Plan/schema", method = RequestMethod.POST, consumes = "application/json")
	public ResponseEntity storeSchema(@RequestHeader HttpHeaders headers, @RequestBody String entity)
			throws ParseException, IOException {
		// Authorize token
		String token = headers.getFirst("Authorization");
		if (token == null) {
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("No Token found!", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		String token1 = "";
		if (token.contains("Bearer ")) {
			token1 = token.substring(7);
			System.out.println("token value is " + token1);
		}
		boolean authorized = authorize(token1);
		if (false == authorized) {
			HttpHeaders httpHeaders = new HttpHeaders();
			return new ResponseEntity("Token is Expired or Invalid Token", httpHeaders, HttpStatus.UNAUTHORIZED);
		} else {
			
			if (entity == null) {
				return new ResponseEntity("No Schema received!", HttpStatus.BAD_REQUEST);
			}
			jedis.set("Plan_schema", entity);
			HttpHeaders httpHeaders = new HttpHeaders();
			return new ResponseEntity("Schema is posted successfully!", httpHeaders, HttpStatus.CREATED);
		}

	}
	
	
	
	/***
	 * Method to authorize the token received
	 * 
	 * @param token
	 * @return
	 */
	public boolean authorize(String token) {

		try {

			System.out.println("token coming in authorize" + token);
			String initVector = "RandomInitVector";
			IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
			SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");

			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
			byte[] original = cipher.doFinal(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(token));
			String entityDecoded = new String(original);
			JSONObject object = new JSONObject(entityDecoded);
			Object arrayOfTests = object.get("ttl");
			Calendar calendar = Calendar.getInstance();
			Date date = calendar.getTime();
			String getDate = arrayOfTests.toString();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

			Date end = formatter.parse(getDate);
			Date start = formatter.parse(formatter.format(date));

			System.out.println(start.toString());
			System.out.println(end.toString());

			if (!start.before(end)) {
				System.out.println("The Token Validity has expired");
				return false;
			}

		} catch (Exception e) {
			System.out.println("inside exception---" + e);
			return false;
		}

		return true;
	}
	
	@RequestMapping(value = "/generateToken", method = RequestMethod.GET)
	public ResponseEntity generateToken(@RequestHeader HttpHeaders headers)
			throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, JSONException {

		String initVector = "RandomInitVector";

		JSONObject obj = new JSONObject();
		obj.put("organization", "example.com");
		obj.put("user", "varsha");

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 60);
		Date date = calendar.getTime();
		SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		obj.put("ttl", df.format(date));
		String token = obj.toString();// token created
		System.out.println("Token valus is " + token + " & TTL is :" + obj.get("ttl"));
		IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
		SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
		byte[] encrypted = cipher.doFinal(token.getBytes()); // encrypting token

		String finalToken = org.apache.tomcat.util.codec.binary.Base64.encodeBase64String(encrypted); // encoded
																										// token
																										// (Base64
																										// encoding)

		HttpHeaders httpHeaders = new HttpHeaders();
		return new ResponseEntity(finalToken, httpHeaders, HttpStatus.CREATED);
	}
	
	
	// to read json instance from redis
	@GetMapping("/Plan/{id}")
	public ResponseEntity<String> read(@PathVariable(name="id", required=true) String id, @RequestHeader HttpHeaders httpHeaders) 
	{
		
		String token = httpHeaders.getFirst("Authorization");
		if (token == null) {
			System.out.println("token verification");
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("No Token found!", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		String token1 = "";
		if (token.contains("Bearer ")) {
			token1 = token.substring(7);

			System.out.println("token value is " + token1);
		}

		boolean authorized = authorize(token1);

		System.out.println("Authorized value is " + authorized);

		if (false == authorized) {
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("Token is Expired or Invalid Token", httpHeaders3, HttpStatus.UNAUTHORIZED);
		} 
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		System.out.println("Reading");
		String jsonString = jedisBean.read(id);
		if(jsonString != null)
			return new ResponseEntity<String>(jsonString, headers, HttpStatus.ACCEPTED);
		else
			return new ResponseEntity<String>("Read unsuccessfull", headers, HttpStatus.BAD_REQUEST);
	}
	
	
	public boolean isValid(String schemaFile, String jsonFile) throws ProcessingException, IOException {
		if (ValidationUtils.isJsonValid(schemaFile, jsonFile)) {
			return true;
		} else {
			return false;
		}
	}
	
	// to insert new json instance into redis
	@PostMapping("/Plan")
	public ResponseEntity<String> insert(@RequestBody(required=true) String body, @RequestHeader HttpHeaders httpHeaders) throws ProcessingException, IOException, org.json.simple.parser.ParseException, URISyntaxException {
		
		String token = httpHeaders.getFirst("Authorization");
		if (token == null) {
			System.out.println("token verification");
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("No Token found!", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		String token1 = "";
		if (token.contains("Bearer ")) {
			token1 = token.substring(7);

			System.out.println("token value is " + token1);
		}

		boolean authorized = authorize(token1);
		System.out.println("Authorized value is " + authorized);

		if (false == authorized) {
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("Token is Expired or Invalid Token", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		
		
		String schemaFile = jedis.get("Plan_schema");
		if (schemaFile == null) {
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("No Schema present to validate the JSON", httpHeaders3,
					HttpStatus.BAD_REQUEST);
		}
		
		HttpHeaders httpHeaders2 = new HttpHeaders();
		boolean flag = isValid(schemaFile.toString(), body);
		
		if(flag){
			JSONObject jsonObject = new JSONObject(body);
			String uuid = jedisBean.insert(jsonObject);
			//demo-3
			elasticSearchConnect.runTask(uuid, body);
			//end of demo-3
			return new ResponseEntity<String>("plan inserted with id "+uuid, HttpStatus.ACCEPTED);
		}
		else {
			return new ResponseEntity<String>("invalid entity", HttpStatus.BAD_REQUEST);
		}
		
			
	}
	
	
	// to delete json instance with key id from redis
	@DeleteMapping("/Plan/{id}")
	public ResponseEntity<String> delete(@PathVariable(name="id", required=true) String id, @RequestHeader HttpHeaders httpHeaders) throws ClientProtocolException, URISyntaxException, IOException {
		
		String token = httpHeaders.getFirst("Authorization");
		if (token == null) {
			System.out.println("token verification");
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("No Token found!", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		String token1 = "";
		if (token.contains("Bearer ")) {
			token1 = token.substring(7);

			System.out.println("token value is " + token1);
		}

		boolean authorized = authorize(token1);
		System.out.println("Authorized value is " + authorized);

		if (false == authorized) {
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("Token is Expired or Invalid Token", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}

//		jedis.connect();

		if (id.isEmpty() || id == null) {
			return new ResponseEntity("Id is empty or null", HttpStatus.NOT_FOUND);
		}
		Set<String> planId = jedis.keys(id);
		if (planId.isEmpty()) {
			return new ResponseEntity("Data Not Found", HttpStatus.NOT_FOUND);
		}
		
		//demo -3
//		for(String key : planId){
//			jedis.del(id);
//		}
//		jedis.del(id); //demo-2
//		String pat = id +"*";
//		Set<String> keys = jedis.keys(pat);
//		for(String lps : keys) {
//			System.out.println("Keys "+lps);
//		}
//		for (String key : keys) {
//		    jedis.del(key);
//		}
		jedisBean.delete(id);
		elasticSearchConnect.deleteTask(id);
		//eod demo-3
		return new ResponseEntity("Data has been deleted successfully", HttpStatus.ACCEPTED);

	}
	
	
	// to update Json instance with key id in Redis
	@PutMapping("/Plan")
	public ResponseEntity<String> update(@RequestBody(required=true) String body, @RequestHeader HttpHeaders httpHeaders) throws ClientProtocolException, org.json.simple.parser.ParseException, IOException, URISyntaxException {
		
		String token = httpHeaders.getFirst("Authorization");
		if (token == null) {
			System.out.println("token verification");
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("No Token found!", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		String token1 = "";
		if (token.contains("Bearer ")) {
			token1 = token.substring(7);

			System.out.println("token value is " + token1);
		}

		boolean authorized = authorize(token1);
		System.out.println("Authorized value is " + authorized);

		if (false == authorized) {
			HttpHeaders httpHeaders3 = new HttpHeaders();
			return new ResponseEntity("Token is Expired or Invalid Token", httpHeaders3, HttpStatus.UNAUTHORIZED);
		}
		String schema = jedis.get("Plan_schema");
		if(schema == null)
			return new ResponseEntity<String>("schema file not found exception", HttpStatus.BAD_REQUEST);
		
		System.out.println("Schema retreived succesfully");
		JSONObject jsonObject = new JSONObject(body);
		if(!jedisBean.update(jsonObject))
			return new ResponseEntity<String>("Failed to update JSON instance in Redis", HttpStatus.BAD_REQUEST);
//		elasticSearchConnect.runTask(uuid, body);
		String uuid = jsonObject.getString("objectId");
		elasticSearchConnect.runTask(uuid, body);
		System.out.println("");
		return new ResponseEntity<String>("usecase updated successfully!", HttpStatus.ACCEPTED);
	
	}
}
