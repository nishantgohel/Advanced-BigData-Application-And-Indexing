package com.example.demo.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ElasticSearchConnect {
	private static final String indexerURI = "http://localhost:9200/planindex";
	
	
	public ElasticSearchConnect(){}
	
	//index the data
	public void runTask(String id, String x) throws ParseException, ClientProtocolException, IOException, URISyntaxException
	{
		
		JSONParser parser = new JSONParser();
		HashMap object = (HashMap)parser.parse(x);
		String objectID = (String) object.get("objectId");
		String objectType = (String) object.get("objectType");
		//String indexer = "/planindexpc"+"/"+objectType+"/"+id;
		String indexer = "/planindexpc"+"/"+"_doc"+"/"+id;
		System.out.println(indexer);
		URL url = new URL("http", "localhost", 9200, indexer);
		JSONObject jsonObject =  (JSONObject) new JSONParser().parse(x);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpPost postRequest = new HttpPost(url.toURI());
		StringEntity entity = new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);
		postRequest.setEntity(entity);
		CloseableHttpResponse httpRespose = httpClient.execute(postRequest);		
		System.out.println(httpRespose.getEntity());
	}
	
	//delete the index
	public void deleteTask(String id) throws URISyntaxException, ClientProtocolException, IOException{
		String[] planId = id.split("____");
		String indexer = "/planindexpc"+"/"+"_doc"+"/"+planId[1];
		URL url = new URL("http", "localhost", 9200, indexer);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		HttpDelete deleteRequest = new HttpDelete(url.toURI());
		CloseableHttpResponse httpRespose = httpClient.execute(deleteRequest);
		System.out.println(httpRespose.getEntity());
		 
	}
	
}
