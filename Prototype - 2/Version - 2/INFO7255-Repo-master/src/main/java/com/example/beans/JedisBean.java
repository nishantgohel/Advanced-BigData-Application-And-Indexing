package com.example.beans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.lettuce.core.RedisException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

public class JedisBean {

	private static final String redisHost = "localhost";
	private static final Integer redisPort = 6379;
	private static JedisPool pool = null;
	private static final String SEP = "____";
	
	public JedisBean() {
		pool = new JedisPool(redisHost, redisPort);
	}
	
	// methods for schema insertion, deletion, read
	public boolean insertSchema(String schema) {
		try {
			Jedis jedis = pool.getResource();
			if(jedis.set("plan_schema", schema).equals("OK"))
				return true;
			else
				return false;
		} catch (JedisException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public String getSchema() {
		try {
			Jedis jedis = pool.getResource();
			return jedis.get("plan_schema");
		} catch(JedisException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// insert plan
	public String insert(JSONObject jsonObject) {
		String idOne = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");
		if(insertUtil(jsonObject, idOne))
			return jsonObject.getString("objectId");
		else
			return null;
	}
	
	private boolean insertUtil(JSONObject jsonObject, String uuid) {
		
		try {
			Jedis jedis = pool.getResource();
			Map<String,String> simpleMap = new HashMap<String,String>();
			
			for(Object key : jsonObject.keySet()) {
				String attributeKey = String.valueOf(key);
				Object attributeVal = jsonObject.get(String.valueOf(key));
				String edge = attributeKey;
				if(attributeVal instanceof JSONObject) {
					
					JSONObject embdObject = (JSONObject) attributeVal;
					String setKey = uuid + SEP + edge;
					String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
					jedis.sadd(setKey, embd_uuid);
					insertUtil(embdObject, embd_uuid);
					
				} else if (attributeVal instanceof JSONArray) {
					
					JSONArray jsonArray = (JSONArray) attributeVal;
					Iterator<Object> jsonIterator = jsonArray.iterator();
					String setKey = uuid + SEP + edge;
					
					while(jsonIterator.hasNext()) {
						JSONObject embdObject = (JSONObject) jsonIterator.next();
						String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
						jedis.sadd(setKey, embd_uuid);
						insertUtil(embdObject, embd_uuid);
					}
					
				} else {
					simpleMap.put(attributeKey, String.valueOf(attributeVal));
				}
			}
			jedis.hmset(uuid, simpleMap);
			jedis.close();
		}
		catch(JedisException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/*
	private Map<String,String> handleObjectAsMap(JSONObject jsonObject) {
		Map<String,String> map = new HashMap<String, String>();
		for(String key : jsonObject.keySet()) {
			map.put(key, jsonObject.get(key).toString());
		}
		return map;
	}
	
	private Set<String> handleObjectAsArray(JSONArray jsonArray) {
		Set<String> set = new HashSet<String>();
		for(Object o : jsonArray) {
			JSONObject ob = (JSONObject) o;
			set.add(ob.toString());
		}
		return set;
	}
	*/
	
	// delete plan
	public boolean delete(String id) 
	{
		//return deleteUtil("plan" + SEP + id);
		return deleteUtil(id);
	}
	
	public boolean deleteUtil(String uuid)
	{
		try 
		{
			Jedis jedis = pool.getResource();
			
			// recursively deleting all embedded json objects
//			Set<String> keys = jedis.keys(uuid+SEP+"*");
//			for(String key : keys) {
//				Set<String> jsonKeySet = jedis.smembers(key);
//				for(String embd_uuid : jsonKeySet) {
//					deleteUtil(embd_uuid);
//				}
//				jedis.del(key);
//			}
			
			//deleting simple fields
			
			Set<String> plankey = jedis.keys(uuid);
			if (plankey.isEmpty())
			{
				return false;
			}
			jedis.del(uuid);
			jedis.close();
			return true;
		} 
		catch(JedisException e) 
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public String read(String id) {
		
			System.out.println("Calling readutil");
			JSONObject jsonObject = readUtil("plan" + SEP + id);
			if(jsonObject != null)
				return jsonObject.toString();
			else
				return null;
	}
	
	private JSONObject readUtil(String uuid) {
		try {
			Jedis jedis = pool.getResource();
			JSONObject o = new JSONObject();
			System.out.println("Reading keys from pattern");
			Set<String> keys = jedis.keys(uuid+SEP+"*");

			// object members
			for(String key : keys) {
				Set<String> jsonKeySet = jedis.smembers(key);
				
				if(jsonKeySet.size() > 1) {
					
					JSONArray ja = new JSONArray();
					Iterator<String> jsonKeySetIterator = jsonKeySet.iterator();
					while(jsonKeySetIterator.hasNext()) {
						ja.put(readUtil(jsonKeySetIterator.next()));
					}
					o.put(key.substring(key.lastIndexOf(SEP)+4), ja);
				} else {
					
					Iterator<String> jsonKeySetIterator = jsonKeySet.iterator();
					JSONObject embdObject = null;
					while(jsonKeySetIterator.hasNext()) {
						embdObject = readUtil(jsonKeySetIterator.next());
					}
					o.put(key.substring(key.lastIndexOf(SEP)+4), embdObject);
					
				}
				
				
				/*
				if(jedis.type(key).equalsIgnoreCase("set")) {
					JSONArray ja = new JSONArray();
					Set<String> set = jedis.smembers(key);
					for(String member : set) {
						ja.put(new JSONObject(member));
					}
					o.put(key.substring(uuid.length()+1), ja);
				} else if (jedis.type(key).equalsIgnoreCase("hash")) {
					Map<String, String> map = jedis.hgetAll(key);
					JSONObject n = new JSONObject();
					for(String k : map.keySet()) {
						n.put(k, map.get(k));
					}
					o.put(key.substring(uuid.length()+1), n);
				} else {
					o.put(key.substring(uuid.length()+1), jedis.get(key));
				}
				*/
			}
			
			// simple members
			Map<String,String> simpleMap = jedis.hgetAll(uuid);
			for(String simpleKey : simpleMap.keySet()) {
				o.put(simpleKey, simpleMap.get(simpleKey));
			}
			
			jedis.close();
			return o;
		} catch(RedisException e) {
			e.printStackTrace();
            return null;
		}
	}
	
	public boolean doesKeyExist(String id) {
		
		try {
			Jedis jedis = pool.getResource();
			if(jedis.exists(id) || !jedis.keys(id + SEP + "*").isEmpty()) {
				jedis.close();
				return true;
			} else {
				jedis.close();
				return false;
			}
		} catch(JedisException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean update(JSONObject jsonObject) {
		try {
			Jedis jedis = pool.getResource();
			String uuid = jsonObject.getString("objectType") + SEP + jsonObject.getString("objectId");
			Map<String,String> simpleMap = jedis.hgetAll(uuid);
			if(simpleMap.isEmpty()) {
				simpleMap = new HashMap<String,String>();
			}
			
			/*
			if(!doesKeyExist(uuid))
				return false;
			*/
			
			for(Object key : jsonObject.keySet()) {
				String attributeKey = String.valueOf(key);
				Object attributeVal = jsonObject.get(String.valueOf(key));
				String edge = attributeKey;
				
				if(attributeVal instanceof JSONObject) {
					
					JSONObject embdObject = (JSONObject) attributeVal;
					String setKey = uuid + SEP + edge;
					String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
					jedis.sadd(setKey, embd_uuid);
					update(embdObject);
					
				} else if (attributeVal instanceof JSONArray) {
					
					JSONArray jsonArray = (JSONArray) attributeVal;
					Iterator<Object> jsonIterator = jsonArray.iterator();
					String setKey = uuid + SEP + edge;
					
					while(jsonIterator.hasNext()) {
						JSONObject embdObject = (JSONObject) jsonIterator.next();
						String embd_uuid = embdObject.get("objectType") + SEP + embdObject.getString("objectId");
						jedis.sadd(setKey, embd_uuid);
						update(embdObject);
					}
					
				} else {
					simpleMap.put(attributeKey, String.valueOf(attributeVal));
				}
			}
			jedis.hmset(uuid, simpleMap);
			jedis.close();
			return true;
			
		} catch(JedisException e) {
			e.printStackTrace();
			return false;
		}
	}
	
}	

