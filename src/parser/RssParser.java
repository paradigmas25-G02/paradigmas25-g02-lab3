package parser;

import feed.Article;
import feed.Feed;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* Esta clase implementa el parser de feed de tipo rss (xml)
 * https://www.tutorialspoint.com/java_xml/java_dom_parse_document.htm
 * */

public class RssParser extends GeneralParser {
	private InputStream input;
	private DocumentBuilderFactory factory;
	private DocumentBuilder docBuilder;
	private Document xmldoc;
	private String sitename;

	public RssParser(InputStream input, String sitename) {
		this.input = input;
		this.factory = DocumentBuilderFactory.newInstance();
		this.sitename = sitename;
		try {
			this.docBuilder = factory.newDocumentBuilder();
			this.xmldoc = docBuilder.parse(this.input);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void addSiteName(Feed feed) {
		NodeList siteNodeList = xmldoc.getElementsByTagName("title");
		Node siteNameNode = siteNodeList.item(0);
		String siteName = siteNameNode.getTextContent();
		feed.setSiteName(siteName);
	}

	private void addArticles(Feed feed) {
		NodeList itemNodeList = xmldoc.getElementsByTagName("item");
		for (int i = 0; i < itemNodeList.getLength(); i++) {
			Node itemNode = itemNodeList.item(i);

			if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
				Element articleElement = (Element) itemNode;

				String title = articleElement.getElementsByTagName("title").item(0).getTextContent();
				String text = articleElement.getElementsByTagName("description").item(0).getTextContent();
				String pubDateString = articleElement.getElementsByTagName("pubDate").item(0).getTextContent();

				SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
				Date publicationDate;
				try {
					publicationDate = formatter.parse(pubDateString);
					String link = articleElement.getElementsByTagName("link").item(0).getTextContent();
	
					Article newArticle = new Article(title, text, publicationDate, link);
					feed.addArticle(newArticle);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Agrego el articulo al feed correspondiente
			}
		}
	}


	@Override
	public Feed parseFeed() {
		Feed parsedFeed = new Feed(this.sitename);
		addSiteName(parsedFeed);
		addArticles(parsedFeed);
		return parsedFeed;
	}

	private static Feed parseFeed(InputStream stream, String sitename) {
		Feed parsedFeed = new Feed(sitename);
		// Esto va de por ley por libreria java, cuestiones de decision de ellos viste
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			Document xmldoc = docBuilder.parse(stream);
			
			// xmldoc.getDocumentElement().normalize();
			// Feed parsedFeed = new Feed();
			

			// 1) Agrego sitename correspondiente
			NodeList siteNodeList = xmldoc.getElementsByTagName("title");
			Node siteNameNode = siteNodeList.item(0);
			String siteName = siteNameNode.getTextContent();
			parsedFeed.setSiteName(siteName);

			// 2) Obtengo los articulos y los muestro por pantalla
			NodeList itemNodeList = xmldoc.getElementsByTagName("item");
			for (int i = 0; i < itemNodeList.getLength(); i++) {
				Node itemNode = itemNodeList.item(i);

				if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
					Element articleElement = (Element) itemNode;

					String title = articleElement.getElementsByTagName("title").item(0).getTextContent();
					String text = articleElement.getElementsByTagName("description").item(0).getTextContent();
					String pubDateString = articleElement.getElementsByTagName("pubDate").item(0).getTextContent();

					SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
					Date publicationDate = formatter.parse(pubDateString);

					String link = articleElement.getElementsByTagName("link").item(0).getTextContent();

					Article newArticle = new Article(title, text, publicationDate, link);
					// printeo el articulo
					// newArticle.prettyPrint();
					// Agrego el articulo al feed correspondiente
					parsedFeed.addArticle(newArticle);
				}
			}

		} catch (ParserConfigurationException e) {
			e.printStackTrace(); // TODO: Agregar error handling mas detallado.
		} catch (SAXException e) {
			e.printStackTrace(); // TODO: Agregar error handling mas detallado.
		} catch (IOException e) {
			e.printStackTrace(); // TODO: Agregar error handling mas detallado.
		} catch (DOMException e) {
			e.printStackTrace(); // TODO: Agregar error handling mas detallado.
		} catch (ParseException e) {
			e.printStackTrace(); // TODO: Agregar error handling mas detallado.
		}

		return parsedFeed;
	}

	public static void main(String[] args) {
		String filepath = "test_files/ejemplo.xml";
		File file = new File(filepath);
		
		System.out.println(file.exists());
		
		try {
			System.out.println("RssParser main method");
			
			byte[] fileBytes = Files.readAllBytes(file.toPath());
			// System.out.println(new String(fileBytes, StandardCharsets.UTF_8));

			// now parse
			InputStream stream = new ByteArrayInputStream(fileBytes);

			Feed newFeed = parseFeed(stream, "Ejemplo");
			newFeed.prettyPrint();
			// newFeed.toString();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
