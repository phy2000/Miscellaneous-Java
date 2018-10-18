package testhttp;
/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

//package org.apache.http.examples.client;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;

public class KafkaRestConsumer {
	static final String strBaseUrl = "http://quickstart:8082";

	public static void main(String[] args) throws Exception {
		CloseableHttpResponse response = null;
//		List<NameValuePair> headers = new ArrayList<NameValuePair>();
		List<Header> headers = new ArrayList<Header>();
		String strDestination;
		String strJson;
		
		// Post a message
//		curl -X POST -H "Content-Type: application/vnd.kafka.json.v2+json" \
//	      -H "Accept: application/vnd.kafka.v2+json" \
//	      --data '{"records":[{"value":{"foo":"bar"}}]}' "http://quickstart:8082/topics/jsontest"

		strDestination = strBaseUrl + "/topics/jsontest";
		strJson = "{\"records\":[{\"value\":{\"foo\":\"bar\"}}]}";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.json.v2+json"));
		headers.add(new BasicHeader(HttpHeaders.ACCEPT, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("post", strDestination, null, headers, strJson);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}
		
		// Create a consumer

//		curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" \
//	      --data '{"name": "my_consumer_instance", "format": "json", "auto.offset.reset": "earliest"}' \
//	      http://quickstart:8082/consumers/my_json_consumer
//	    	  
		strDestination = strBaseUrl + "/consumers/httpclient_consumer";
		strJson = "{\"name\":\"httpclient_consumer_instance\",\"format\":\"json\",\"auto.offset.reset\":\"earliest\"}";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("post", strDestination, null, headers, strJson);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}

		// Subscribe consumer to topic jsontest
//		curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" --data '{"topics":["jsontest"]}' \
//		 http://quickstart:8082/consumers/my_json_consumer/instances/my_consumer_instance/subscription

		strDestination = strBaseUrl + "/consumers/httpclient_consumer/instances/httpclient_consumer_instance/subscription";
		strJson = "{\"topics\":[\"jsontest\"]}";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("post", strDestination, null, headers, strJson);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}

		// Get messages from beginning
//		curl -X GET -H "Accept: application/vnd.kafka.json.v2+json" \
//	      http://quickstart:8082/consumers/my_json_consumer/instances/my_consumer_instance/records
		strDestination = strBaseUrl + "/consumers/httpclient_consumer/instances/httpclient_consumer_instance/records";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.ACCEPT, "application/vnd.kafka.json.v2+json"));
		response = HttpUtil.sendRequest("get", strDestination, null, headers, null);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}

		// Remove the consumer instance
//		curl -X DELETE -H "Content-Type: application/vnd.kafka.v2+json" \
//	      http://quickstart:8082/consumers/my_json_consumer/instances/my_consumer_instance
		strDestination = strBaseUrl + "/consumers/httpclient_consumer/instances/httpclient_consumer_instance";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("delete", strDestination, null, headers, null);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}
	}
}