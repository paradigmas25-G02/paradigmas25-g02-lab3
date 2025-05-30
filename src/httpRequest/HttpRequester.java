package httpRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/* Esta clase se encarga de realizar efectivamente el pedido de feed al servidor de noticias
 * Leer sobre como hacer una http request en java
 * https://www.baeldung.com/java-http-request
 * */

public class HttpRequester {

	private int connectTimeout;
	private int readTimeout;

	public HttpRequester() {
		this.connectTimeout = 5000;
		this.readTimeout = 5000;
	}

	public HttpRequester(int connectTimeout, int readTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	public String getFeedRss(String urlFeed) throws IOException, RequestException {
		return getData(urlFeed, "application/rss+xml");
	}

	private String getData(String urlFeed, String mimetype)
			throws MalformedURLException, IOException, ProtocolException, RequestException {
		String feedRssXml = null;
		HttpURLConnection connection = null;

		try {
			URL url = new URL(urlFeed);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", mimetype);
			connection.setConnectTimeout(connectTimeout); // 5 seconds timeout
			connection.setReadTimeout(readTimeout); // 5 seconds read timeout

			int status = connection.getResponseCode();

			if (status == HttpURLConnection.HTTP_OK) {
				feedRssXml = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			} else {
				throw new RequestException(status, urlFeed);
			}
		} finally {
			if (connection != null)
				connection.disconnect();
		}

		return feedRssXml;
	}

	public String getFeedReddit(String urlFeed) throws IOException, RequestException {
		return getData(urlFeed, "application/json");
	}

	public static void main(String[] args) throws IOException, RequestException {

		HttpRequester a = new HttpRequester(10000,10000);
		System.out.println(a.getFeedRss("https://rss.nytimes.com/services/xml/rss/nyt/Business.xml"));

		// HttpRequester b = new HttpRequester();
		// System.out.println(b.getFeedReddit("https://www.reddit.com/r/Sales/hot/.json?count=100"));

	}
}
