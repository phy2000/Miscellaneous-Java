package testhttp;

import org.apache.http.*;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * A simple Java REST GET example using the Apache HTTP library.
 * This executes a call against the Yahoo Weather API service, which is
 * actually an RSS service (http://developer.yahoo.com/weather/).
 * 
 * Try this Twitter API URL for another example (it returns JSON results):
 * http://search.twitter.com/search.json?q=%40apple
 * (see this url for more twitter info: https://dev.twitter.com/docs/using-search)
 * 
 * Apache HttpClient: http://hc.apache.org/httpclient-3.x/
 *
 */
public class ApacheHttpRestClient1 {

  public static void main(String[] args) {
    DefaultHttpClient httpclient = new DefaultHttpClient();
    try {
      // specify the host, protocol, and port
//        HttpHost target = new HttpHost("weather.yahooapis.com", 80, "http");
        HttpHost target = new HttpHost("weather.yahooapis.com", 80, "https");
      
      // specify the get request
      HttpGet getRequest = new HttpGet("/forecastrss?p=80020&u=f");

      System.out.println("executing request to " + target);

      HttpResponse httpResponse = httpclient.execute(target, getRequest);
      HttpEntity entity = httpResponse.getEntity();

      System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
      System.out.println(httpResponse.getStatusLine());
      Header[] headers = httpResponse.getAllHeaders();
      for (int i = 0; i < headers.length; i++) {
        System.out.println(headers[i]);
      }
      System.out.println("----------------------------------------");

      if (entity != null) {
        System.out.println(EntityUtils.toString(entity));
      }
      System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<\n");

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      // When HttpClient instance is no longer needed,
      // shut down the connection manager to ensure
      // immediate deallocation of all system resources
      httpclient.getConnectionManager().shutdown();
      httpclient.close();
    }
  }
}