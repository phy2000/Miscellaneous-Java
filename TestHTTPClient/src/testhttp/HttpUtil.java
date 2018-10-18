/**
 * 
 */
package testhttp;

import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * @author pyoung
 *
 */
class HttpUtil {
	static CloseableHttpResponse sendRequest(String strMethod, String strUri, List<NameValuePair> listParameters,
			List<Header> listHeaders, String strData) throws Exception {
		CloseableHttpResponse response = null;
		strMethod = strMethod.toLowerCase();
		URIBuilder builder = new URIBuilder(strUri);
		CloseableHttpClient httpclient = HttpClients.createDefault();

		System.err.printf("Method:<%s>; URI:<%s>; Data:<%s>\n", strMethod, strUri, strData);

		if (listParameters != null) {
			builder.addParameters(listParameters);
		}
		Header[] headers = listHeaders.toArray(new Header[listHeaders.size()]);
		switch (strMethod) {
		case "post":
			HttpPost post = new HttpPost(builder.build());
			post.setHeaders(headers);
			if (strData != null) {
				StringEntity entity = new StringEntity(strData);
				post.setEntity(entity);
			}
			response = httpclient.execute(post);
			break;
		case "get":
			HttpGet get = new HttpGet(builder.build());
			get.setHeaders(headers);
			response = httpclient.execute(get);
			break;
		case "delete":
			HttpDelete delete = new HttpDelete(builder.build());
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
