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
		
		// Configurar factory para manejar namespaces (importante para feeds con Atom)
		this.factory.setNamespaceAware(true);
		
		try {
			this.docBuilder = factory.newDocumentBuilder();
			this.xmldoc = docBuilder.parse(this.input);
			this.xmldoc.getDocumentElement().normalize();
		} catch (ParserConfigurationException e) {
			System.err.println("Error de configuración del parser para: " + sitename);
			e.printStackTrace();
			throw new RuntimeException("No se pudo configurar el parser XML", e);
		} catch (SAXException e) {
			System.err.println("Error de parsing XML para: " + sitename);
			e.printStackTrace();
			throw new RuntimeException("Error parsing XML feed", e);
		} catch (IOException e) {
			System.err.println("Error de E/O para: " + sitename);
			e.printStackTrace();
			throw new RuntimeException("Error leyendo feed", e);
		}
	}

	private void addSiteName(Feed feed) {
		try {
			NodeList siteNodeList = xmldoc.getElementsByTagName("title");
			if (siteNodeList.getLength() > 0) {
				Node siteNameNode = siteNodeList.item(0);
				if (siteNameNode != null) {
					String siteName = siteNameNode.getTextContent();
					if (siteName != null && !siteName.trim().isEmpty()) {
						feed.setSiteName(siteName.trim());
						return;
					}
				}
			}
			
			// Fallback: usar el sitename pasado en el constructor
			feed.setSiteName(this.sitename);
		} catch (Exception e) {
			// En caso de error, usar el sitename del constructor
			System.out.println("Error obteniendo nombre del sitio: " + e.getMessage());
			feed.setSiteName(this.sitename);
		}
	}

	private void addArticles(Feed feed) {
		NodeList itemNodeList = xmldoc.getElementsByTagName("item");
		for (int i = 0; i < itemNodeList.getLength(); i++) {
			Node itemNode = itemNodeList.item(i);

			if (itemNode.getNodeType() == Node.ELEMENT_NODE) {
				Element articleElement = (Element) itemNode;

				try {
					// Extracción robusta de title
					String title = getElementTextSafely(articleElement, "title");
					if (title == null || title.trim().isEmpty()) {
						System.out.println("Artículo sin título válido, saltando...");
						continue;
					}

					// Extracción robusta de description
					String text = getElementTextSafely(articleElement, "description");
					if (text == null) {
						// Intentar con contenido alternativo (summary, content, etc.)
						text = getElementTextSafely(articleElement, "summary");
						if (text == null) {
							text = getElementTextSafely(articleElement, "content");
							if (text == null) {
								text = ""; // Descripción vacía si no hay contenido
							}
						}
					}

					// Extracción robusta de fecha de publicación
					Date publicationDate = parsePublicationDate(articleElement);
					if (publicationDate == null) {
						// Usar fecha actual si no se puede parsear
						publicationDate = new Date();
					}

					// Extracción robusta de link
					String link = getElementTextSafely(articleElement, "link");
					if (link == null) {
						// Intentar obtener link desde elementos Atom
						link = getAtomLink(articleElement);
						if (link == null) {
							link = getElementTextSafely(articleElement, "guid");
							if (link == null) {
								link = ""; // Link vacío si no hay
							}
						}
					}

					Article newArticle = new Article(title, text, publicationDate, link);
					feed.addArticle(newArticle);

				} catch (Exception e) {
					System.out.println("Error procesando artículo: " + e.getMessage());
					// Continuar con el siguiente artículo en lugar de fallar completamente
					continue;
				}
			}
		}
	}

	/**
	 * Extrae texto de un elemento de forma segura, manejando casos donde el elemento no existe
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

	/**
	 * Intenta extraer un link desde elementos Atom (para feeds híbridos RSS+Atom)
	 */
	private String getAtomLink(Element articleElement) {
		try {
			// Buscar <atom:link href="..."/>
			NodeList atomLinks = articleElement.getElementsByTagName("atom:link");
			if (atomLinks.getLength() > 0) {
				Element atomLink = (Element) atomLinks.item(0);
				return atomLink.getAttribute("href");
			}
		} catch (Exception e) {
			// Ignorar errores
		}
		return null;
	}

	/**
	 * Parsea la fecha de publicación con múltiples formatos
	 */
	private Date parsePublicationDate(Element articleElement) {
		// Intentar obtener fecha desde pubDate (RSS estándar)
		String pubDateString = getElementTextSafely(articleElement, "pubDate");
		if (pubDateString != null) {
			Date date = parseDate(pubDateString, "EEE, dd MMM yyyy HH:mm:ss Z");
			if (date != null) return date;
			
			// Intentar formato alternativo
			date = parseDate(pubDateString, "yyyy-MM-dd'T'HH:mm:ss'Z'");
			if (date != null) return date;
		}

		// Intentar obtener fecha desde published (Atom)
		String publishedString = getElementTextSafely(articleElement, "published");
		if (publishedString != null) {
			Date date = parseDate(publishedString, "yyyy-MM-dd'T'HH:mm:ss'Z'");
			if (date != null) return date;
		}

		// Intentar obtener fecha desde updated (Atom)
		String updatedString = getElementTextSafely(articleElement, "updated");
		if (updatedString != null) {
			Date date = parseDate(updatedString, "yyyy-MM-dd'T'HH:mm:ss'Z'");
			if (date != null) return date;
		}

		return null; // No se pudo parsear fecha
	}

	/**
	 * Parsea una fecha con un formato específico
	 */
	private Date parseDate(String dateString, String format) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat(format, Locale.ENGLISH);
			return formatter.parse(dateString);
		} catch (ParseException e) {
			return null;
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
