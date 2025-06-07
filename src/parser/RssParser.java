package parser;

import feed.Article;
import feed.Feed;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;


import java.io.IOException;
import java.io.InputStream;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Esta clase implementa un parser de feeds que soporta tanto RSS como Atom.
 * Determina el formato del feed y extrae los artículos de manera robusta.
 */
public class RssParser extends GeneralParser {
	private final InputStream input;
	private final Document xmldoc;
	private final String initialSiteName;

	public RssParser(InputStream input, String sitename) {
		this.input = input;
		this.initialSiteName = sitename;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true); // Soporte para namespaces (Atom)
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			this.xmldoc = docBuilder.parse(this.input);
			this.xmldoc.getDocumentElement().normalize();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			// Lanza una excepción en caso de error crítico de parsing
			throw new RuntimeException("Error parsing XML feed for: " + sitename, e);
		}
	}

	@Override
	public Feed parseFeed() {
		Feed parsedFeed = new Feed(this.initialSiteName);
		addSiteName(parsedFeed);
		addArticles(parsedFeed);
		return parsedFeed;
	}

	/**
	 * Extrae el nombre del sitio del feed, con fallback al nombre inicial.
	 */
	private void addSiteName(Feed feed) {
		try {
			// El título principal está en <channel><title> para RSS y <feed><title> para Atom.
			NodeList siteNodeList = xmldoc.getElementsByTagName("title");
			if (siteNodeList.getLength() > 0) {
				Node siteNameNode = siteNodeList.item(0);
				if (siteNameNode != null) {
					String siteName = siteNameNode.getTextContent();
					if (siteName != null && !siteName.trim().isEmpty()) {
						feed.setSiteName(siteName.trim());
						return; // Nombre encontrado y asignado
					}
				}
			}


		} catch (Exception e) {
			// Ignorar error y usar el fallback
		}
		// Fallback si no se encuentra el título en el XML
		feed.setSiteName(this.initialSiteName);
	}
	
	/**
	 * Extrae los artículos del feed, manejando tanto RSS (<item>) como Atom (<entry>).
	 */
	private void addArticles(Feed feed) {
		// Intenta con la etiqueta de artículo de RSS ("item") primero.
		NodeList articleNodeList = xmldoc.getElementsByTagName("item");
		
		// Si no se encuentran "item", intenta con la etiqueta de Atom ("entry").
		if (articleNodeList.getLength() == 0) {
			articleNodeList = xmldoc.getElementsByTagName("entry");
		}
		
		for (int i = 0; i < articleNodeList.getLength(); i++) {
			Node itemNode = articleNodeList.item(i);

			if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
				Element articleElement = (Element) itemNode;

				String title = getElementTextSafely(articleElement, "title");
				if (title == null || title.trim().isEmpty()) {
					continue; // Salta artículos sin título
				}
				
				String content = getArticleContent(articleElement);
				Date publicationDate = parsePublicationDate(articleElement);
				String link = getArticleLink(articleElement);

				Article newArticle = new Article(title, content, publicationDate, link);
				feed.addArticle(newArticle);
			}
		}
	}

	/**
	 * Extrae el contenido del artículo, probando varias etiquetas comunes.
	 */
	private String getArticleContent(Element parent) {
		String content = getElementTextSafely(parent, "description"); // RSS
		if (content == null) {
			content = getElementTextSafely(parent, "content"); // Atom
		}
		if (content == null) {
			content = getElementTextSafely(parent, "summary"); // Atom (alternativo)
		}
		return (content != null) ? content : ""; // Devuelve vacío si no se encuentra
	}

	/**
	 * Extrae el link del artículo, manejando el formato de RSS y Atom.
	 */
	private String getArticleLink(Element parent) {
		String link = getElementTextSafely(parent, "link"); // Intenta leer el texto del link (RSS)
		
		if (link == null || link.isEmpty()) {
			// Si falla, intenta leer el atributo 'href' del link (Atom)
			try {
				NodeList linkNodes = parent.getElementsByTagName("link");
				if (linkNodes.getLength() > 0 && linkNodes.item(0).hasAttributes()) {
					Node hrefAttr = linkNodes.item(0).getAttributes().getNamedItem("href");
					if (hrefAttr != null) {
						link = hrefAttr.getNodeValue();
					}
				}
			} catch (Exception e) { /* Ignorar error */ }
		}
		
		if (link == null || link.isEmpty()) {
			link = getElementTextSafely(parent, "guid"); // Fallback a guid
		}
		
		return (link != null) ? link : "";
	}
	
	/**
	 * Parsea la fecha de publicación buscando en etiquetas de RSS y Atom.
	 */
	private Date parsePublicationDate(Element articleElement) {
		String[] dateTags = {"pubDate", "published", "updated"};
		String[] dateFormats = {
			"EEE, dd MMM yyyy HH:mm:ss Z",      // Formato RSS estándar
			"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",   // Formato Atom con milisegundos
			"yyyy-MM-dd'T'HH:mm:ss'Z'",        // Formato Atom sin milisegundos
			"yyyy-MM-dd'T'HH:mm:ssXXX"        // Formato Atom con zona horaria
		};

		for (String tag : dateTags) {
			String dateString = getElementTextSafely(articleElement, tag);
			if (dateString != null) {
				for (String format : dateFormats) {
					Date date = parseDate(dateString, format);
					if (date != null) {
						return date;
					}
				}
			}
		}

		return new Date(); // Fallback a la fecha actual si no se encuentra/parsea
	}

	/**
	 * Parsea una fecha con un formato específico de forma segura.
	 */
	private Date parseDate(String dateString, String format) {
		try {
			return new SimpleDateFormat(format, Locale.ENGLISH).parse(dateString);
		} catch (ParseException e) {
			return null; // Devuelve null si el formato no coincide
		}
	}
	
	/**
	 * Extrae texto de un elemento de forma segura, devolviendo null si no existe.
	 */
	private String getElementTextSafely(Element parent, String tagName) {
		try {
			NodeList nodes = parent.getElementsByTagName(tagName);
			if (nodes.getLength() > 0 && nodes.item(0) != null) {
				String content = nodes.item(0).getTextContent();
				return (content != null && !content.trim().isEmpty()) ? content.trim() : null;
			}
		} catch (Exception e) {
			// Ignorar errores y devolver null
		}
		return null;
	}
}