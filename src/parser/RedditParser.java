package parser;

import feed.Article;
import feed.Feed;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Esta clase implementa el parser de feed de tipo reddit (json)
 * pero no es necesario su implementación
 */
public class RedditParser extends GeneralParser {

	private InputStream stream;
	private String sitename;

	/**
	 * Constructor de la clase RedditParser que recibe la ruta del archivo y el nombre del sitio.
	 * @param filepath la ruta del archivo
	 * @param _sitename el nombre del sitio
	 * @throws FileNotFoundException si no se encuentra el archivo
	 */
	public RedditParser(String filepath, String _sitename)
		throws FileNotFoundException {
		stream = new FileInputStream(filepath);
		sitename = _sitename;
	}

	/**
	 * Constructor de la clase RedditParser que recibe un InputStream y el nombre del sitio.
	 * @param _stream el InputStream
	 * @param _sitename el nombre del sitio
	 */
	public RedditParser(InputStream _stream, String _sitename) {
		stream = _stream;
		sitename = _sitename;
	}

	/**
	 * Obtiene la fecha de publicación de un post.
	 * @param post el post en formato JSON
	 * @return la fecha de publicación del post
	 */
	private Date getPublishingDate(JSONObject post) {
		Long timestamp = post.getLong("created_utc");
		return new Date(timestamp);
	}

	/**
	 * Convierte un post en formato JSON a un objeto de tipo Article.
	 * @param post el post en formato JSON
	 * @return el objeto Article correspondiente al post
	 */
	private Article postToArticle(JSONObject post) {
		String title = post.getString("title");
		String text = post.getString("selftext");
		if (text.length() == 0) {
			text = post.getString("url"); // External site links
		}
		Date publicationDate = getPublishingDate(post);
		String link = "https://reddit.com" + post.getString("permalink");
		return new Article(title, text, publicationDate, link);
	}

	@Override
	public Feed parseFeed() {
		Feed feed = new Feed(sitename);
		JSONArray posts = new JSONObject(new JSONTokener(stream))
			.getJSONObject("data")
			.getJSONArray("children");
		for (int i = 0; i < posts.length(); i++) {
			JSONObject post = posts.getJSONObject(i).getJSONObject("data");
			Article article = postToArticle(post);
			feed.addArticle(article);
		}
		return feed;
	}
}
