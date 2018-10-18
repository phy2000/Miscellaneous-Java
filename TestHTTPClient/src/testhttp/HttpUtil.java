/**
 * 
 */
package testhttp;

import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

/**
 * @author pyoung
 *
 */
class HttpUtil {
	static Logger logger = Logger.getLogger(HttpUtil.class.getName());

	static CloseableHttpClient createClient(int timeoutSeconds) {
		return createClient(timeoutSeconds, null, null);
	}
	
	static CloseableHttpClient createClient(int timeoutSeconds, String Username, String Password) {
		if (timeoutSeconds < 0) {
			timeoutSeconds = 1;
		}
		Builder configBuilder = RequestConfig.custom();
		RequestConfig reqConfig = RequestConfig.DEFAULT;
		CredentialsProvider provider = null;
		if (timeoutSeconds > 0) {
			configBuilder.setConnectionRequestTimeout(timeoutSeconds * 1000);
			configBuilder.setSocketTimeout(timeoutSeconds * 1000);
			configBuilder.setConnectTimeout(timeoutSeconds * 1000);
		}
		int connectRequestTimeoutMilli = reqConfig.getConnectionRequestTimeout();
		int socketTimeoutMilli = reqConfig.getSocketTimeout();
		int connectTimeoutMilli = reqConfig.getConnectTimeout();
		logger.info(String.format("ConnectRequestTimeout = %d; SocketTimeout = %d; ConnectTimeout = %s",
				connectRequestTimeoutMilli, socketTimeoutMilli, connectTimeoutMilli));
		if (Username != null && Password != null) {
			provider = new BasicCredentialsProvider();
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(Username, Password);
			provider.setCredentials(AuthScope.ANY,  creds);
		}
		HttpClientBuilder builder = HttpClientBuilder.create();
		builder.setDefaultRequestConfig(reqConfig);
		if (provider != null) {
			builder.setDefaultCredentialsProvider(provider);
		}
		CloseableHttpClient httpclient = builder.build();
		return httpclient;

	}

	static CloseableHttpResponse sendRequest(String strMethod, String strUri) throws Exception {
		return sendRequest(strMethod, strUri, null, null, null);
	}

	static CloseableHttpResponse sendRequest(String strMethod, String strUri, List<NameValuePair> listParameters) throws Exception {
		return sendRequest(strMethod, strUri, listParameters, null, null);
	}

	static CloseableHttpResponse sendRequest(String strMethod, String strUri, List<Header> listHeaders, String strData) throws Exception {
		return sendRequest(strMethod, strUri, null, listHeaders, strData);
	}

	static CloseableHttpResponse sendRequest(String strMethod, String strUri, String strData) throws Exception {
		return sendRequest(strMethod, strUri, null, null, strData);
	}

	static CloseableHttpResponse sendRequest(String strMethod, String strUri, List<NameValuePair> listParameters,
			List<Header> listHeaders, String strData) throws Exception {
		return sendRequest(strMethod, strUri, listParameters,
				listHeaders, strData, null);
	}

	static CloseableHttpResponse sendRequest(String strMethod, String strUri, List<NameValuePair> listParameters,
			List<Header> listHeaders, String strData, CloseableHttpClient paramHttpClient) throws Exception {
		CloseableHttpResponse response = null;
		strMethod = strMethod.toLowerCase();
		URIBuilder uriBuilder = new URIBuilder(strUri);
		CloseableHttpClient httpclient = null;
		
		if (paramHttpClient == null) {
			httpclient = HttpClients.createDefault();
		} else {
			httpclient = paramHttpClient;
		}

		logger.info(String.format("\n\tMethod:<%s>; \n\tURI:<%s>; \n\tData:<%s>\n", strMethod, strUri, strData));

		if (listParameters != null) {
			uriBuilder.addParameters(listParameters);
		}
		Header[] headers = listHeaders.toArray(new Header[listHeaders.size()]);
		switch (strMethod) {
		case "post":
			HttpPost post = new HttpPost(uriBuilder.build());
			post.setHeaders(headers);
			if (strData != null) {
				StringEntity entity = new StringEntity(strData);
				post.setEntity(entity);
			}
			response = httpclient.execute(post);
			break;
		case "get":
			HttpGet get = new HttpGet(uriBuilder.build());
			get.setHeaders(headers);
			response = httpclient.execute(get);
			break;
		case "delete":
			HttpDelete delete = new HttpDelete(uriBuilder.build());
			delete.setHeaders(headers);
			response = httpclient.execute(delete);
			break;
		default:
			return null;
		}

		return response;
	}

	static void printResponse(CloseableHttpResponse response) throws Exception {
		System.out.println(response.getStatusLine());
		HttpEntity entity = response.getEntity();
		Header[] headers = response.getAllHeaders();
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		for (int i = 0; i < headers.length; i++) {
			System.out.println(headers[i]);
		}
		System.out.println("----------------------------------------");

		if (entity != null) {
			System.out.println(EntityUtils.toString(entity));
		}
		System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");
		EntityUtils.consume(entity);
	}
}
