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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;


import org.apache.http.Header;
import org.apache.http.HttpHeaders;


public class OsiSoftPiRestClient {
	static final String strBaseUrl = "http://quickstart:8082";
	static Logger logger;
	static String _progname;
	static OsiSoftPiConfig mConfig = null;

	private static void init_main(String[] args) throws Exception {
		mConfig = new OsiSoftPiConfig();
		_progname = OsiSoftPiRestClient.class.getName();
		logger = Logger.getLogger(_progname);
		int argId;
		LongOpt[] longopts = { new LongOpt("url", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
				new LongOpt("topic", LongOpt.REQUIRED_ARGUMENT, null, 't'),
				new LongOpt("consumer", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
				new LongOpt("verbose", LongOpt.NO_ARGUMENT, null, 'v'),
				new LongOpt("timeout", LongOpt.REQUIRED_ARGUMENT, null, 'T'), };
		Getopt g = new Getopt(_progname, args, "+:u:t:c:v", longopts);
		g.setOpterr(false); // We'll do our own error handling

		while ((argId = g.getopt()) != -1) {
			switch (argId) {
			case 'T':
				mConfig.m_timeoutSeconds = new Integer(g.getOptarg());
				break;
			case 'u':
				mConfig.url = g.getOptarg();
				break;
			case 't':
				// Session ID
				mConfig.topicName = g.getOptarg();
				break;
			case 'c':
				// Port number
				mConfig.consumerName = g.getOptarg();
				break;
			case 'v':
				mConfig.verbose = true;
				break;
			case ':':
				// Missing option arg
				Usage.print(_progname);
				throw new Exception("Missing argument");
				// break;
			case '?':
				// Unknown option
				Usage.print(_progname);
				throw new Exception("Unknown option");
				// break;
			default:
				System.err.println("Unexpected getopt error: <" + argId + ">");
				System.exit(1);
				break;
			} // switch (argId)
		} // while (argId)

	}

	public static void main(String[] args) throws Exception {
		CloseableHttpResponse response = null;
		List<Header> headers = new ArrayList<Header>();
		String strDestination;
		String strJson;

		init_main(args);
		// Create a consumer

		CloseableHttpClient httpclient = HttpUtil.createClient(10);
//		curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" \
//	      --data '{"name": "my_consumer_instance", "format": "json", "auto.offset.reset": "earliest"}' \
//	      http://quickstart:8082/consumers/my_json_consumer
//	    	  
		strDestination = mConfig.url + "/consumers/" + mConfig.consumerName ;
		strJson = String.format("{\"name\":\"%s_instance\",\"format\":\"json\",\"auto.offset.reset\":\"earliest\"}", mConfig.consumerName);
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("post", strDestination, null, headers, strJson, httpclient);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}

		// Subscribe consumer to topic jsontest
//		curl -X POST -H "Content-Type: application/vnd.kafka.v2+json" --data '{"topics":["jsontest"]}' \
//		 http://quickstart:8082/consumers/my_json_consumer/instances/my_consumer_instance/subscription

		strDestination = mConfig.url
				+ "/consumers/httpclient_consumer/instances/httpclient_consumer_instance/subscription";
		strJson = "{\"topics\":[\"jsontest\"]}";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("post", strDestination, null, headers, strJson, httpclient);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}

		// Get messages
		strDestination = mConfig.url + "/consumers/httpclient_consumer/instances/httpclient_consumer_instance/records";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.ACCEPT, "application/vnd.kafka.json.v2+json"));
		response = HttpUtil.sendRequest("get", strDestination, null, headers, null, httpclient);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}

		// Remove the consumer instance
		strDestination = mConfig.url + "/consumers/httpclient_consumer/instances/httpclient_consumer_instance";
		headers.clear();
		headers.add(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.kafka.v2+json"));
		response = HttpUtil.sendRequest("delete", strDestination, null, headers, null, httpclient);
		if (response != null) {
			HttpUtil.printResponse(response);
			response.close();
			response = null;
		}
	}
	static class Usage {
		final static String message = "Usage: %s -u|--url=<url> -t|--topic=<topic> [-c|--consumer=<name>] [-t|--timeout=<seconds>] [-v|--verbose]\n";

		static void print(String progname) {
			System.err.printf(String.format(Usage.message, progname));
			System.exit(1);
		}
	}
	static class OsiSoftPiConfig {
		String url = "http://localhost:8082";
		String topicName = null;
		String consumerName = "pi_consumer";
		boolean verbose = false;
		int m_timeoutSeconds = 5;

		static void print(String progname) {
			System.err.printf(String.format(Usage.message, progname, progname, progname));
			System.exit(1);
		}
	}
}

