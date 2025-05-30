import feed.Article;
import feed.Feed;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import parser.RedditParser;
import parser.RssParser;
import parser.SubscriptionParser;
import subscription.SingleSubscription;
import subscription.Subscription;
import httpRequest.HttpRequester;
import namedEntity.NamedEntity;
import namedEntity.heuristic.Heuristic;
import namedEntity.heuristic.QuickHeuristic;
import namedEntity.heuristic.RandomHeuristic;

public class FeedReaderMain {

	private static void printHelp() {
		System.out.println(
				"Please, call this program in correct way: FeedReader [-rh] o FeedReader [-qh]");
	}

	public static void main(String[] args) {

		List<Feed> collectedFeeds = new ArrayList<>(); // Para almacenar todos los feeds parseados
		System.out.println("************* FeedReader version 1.0 *************");

		if (args.length == 0 || (args.length == 1 && ((args[0].equals("-qh")) || (args[0].equals("-rh"))))) {
			/*
			 * Leer el archivo de suscription por defecto;
			 * Llamar al httpRequester para obtenr el feed del servidor
			 * Llamar al Parser especifico para extrar los datos necesarios por la
			 * aplicacion
			 * Llamar al constructor de Feed
			 */
			try {
				SubscriptionParser subParser = new SubscriptionParser("./config/subscriptions.json");
				Subscription allSubscriptions = subParser.getSubscriptions();
				HttpRequester http_requester = new HttpRequester();

				for (SingleSubscription currentSubscription : allSubscriptions.getSubscriptionsList()) {
					String urlType = currentSubscription.getUrlType();
					String baseUrl = currentSubscription.getUrl();
					List<String> params = currentSubscription.getUrlParams();

					for (String param : params) {
						String finalUrl = baseUrl.replaceAll("%s", param);

						if ("reddit".equalsIgnoreCase(urlType)) {
							String pageData = http_requester.getFeedReddit(finalUrl);
							InputStream jsonStream = new ByteArrayInputStream(
									pageData.getBytes(StandardCharsets.UTF_8));
							RedditParser parser = new RedditParser(jsonStream, finalUrl);
							Feed feed = parser.parseFeed();
							collectedFeeds.add(feed);

						} else if ("rss".equalsIgnoreCase(urlType)) {
							String pageData = http_requester.getFeedRss(finalUrl);
							InputStream xmlStream = new ByteArrayInputStream(pageData.getBytes(StandardCharsets.UTF_8));
							RssParser rssParser = new RssParser(xmlStream, finalUrl);
							Feed feed = rssParser.parseFeed();
							collectedFeeds.add(feed);

						} else {
							System.out.println("    Tipo de suscripción desconocido: " + urlType);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (args.length == 0) {
			// LLamar al prettyPrint del Feed para ver los articulos del feed en forma
			// legible y amigable para el usuario
			for (Feed feed : collectedFeeds) {
				feed.prettyPrint();
			}

		} else if (args.length == 1  && (args[0].equals("-rh") || args[0].equals("-qh"))) {
			/*
			 * Leer el archivo de suscription por defecto;
			 * Llamar al httpRequester para obtenr el feed del servidor
			 * Llamar al Parser especifico para extrar los datos necesarios por la
			 * aplicacion
			 * Llamar al constructor de Feed
			 * Llamar a la heuristica para que compute las entidades nombradas de cada
			 * articulos del feed
			 * LLamar al prettyPrint de la tabla de entidades nombradas del feed.
			 */
			Heuristic heu;
			Map<String, NamedEntity> aggregatedEntities = new HashMap<>();
			if (args[0].equals("-rh")) {
				heu = new RandomHeuristic();}
				else if (args[0].equals("-qh")) {
				heu = new QuickHeuristic();}
			else {
				System.err.println("Comando invalido");
				printHelp();
				return;
			}
				// recorro la lista de feeds y computo heristicas
				for (Feed feed : collectedFeeds) {
					for (Article article : feed.getArticleList()) {
						article.computeNamedEntities(heu);
						// Agregar a la tabla global
                        for (NamedEntity neFromArticle : article.getNamedEntityList()) {
                            String key = neFromArticle.getName() + "::" + neFromArticle.getCategory();
                            NamedEntity globalEntity = aggregatedEntities.get(key);
                            if (globalEntity == null) {
                                // Crear nueva entidad para la tabla agregada, copiando la frecuencia del artículo
                                aggregatedEntities.put(key, new NamedEntity(neFromArticle.getName(), neFromArticle.getFrequency(), neFromArticle.getCategory(), neFromArticle.getTopic()));
                            } else {
                                globalEntity.setFrequency(globalEntity.getFrequency() + neFromArticle.getFrequency());
                            }
                        }


					}
			
			} 
			
			// Imprimir la tabla de entidades agregadas
            System.out.println("\n--- Tabla Agregada de Entidades Nombradas (Global) ---");
            if (aggregatedEntities.isEmpty()) {
                System.out.println("No se encontraron entidades nombradas con la heurística seleccionada.");
            } else {
                List<NamedEntity> sortedEntities = new ArrayList<>(aggregatedEntities.values());
                Collections.sort(sortedEntities, new Comparator<NamedEntity>() {
                    @Override
                    public int compare(NamedEntity e1, NamedEntity e2) {
                        return Integer.compare(e2.getFrequency(), e1.getFrequency()); // Orden descendente
                    }
                });

                for (NamedEntity ne : sortedEntities) {
					// Topic topic = ne.getTopic();
                    if (!ne.getCategory().isOther()) {
                    	ne.prettyPrint();
                    }
                }

				//+--------------------------------------------------------------------------------------------------------------------------------------------+
				// Tomo decisión estetica de eliminar las entidades nombradas que no tienen un topico ya que hace mucha impresión y no es amigable a la mirada
				//+--------------------------------------------------------------------------------------------------------------------------------------------+
				
				
				//  for (NamedEntity ne : sortedEntities) {
				// 	Topic topic = ne.getTopic();	
                //     if (ne.getCategory().equals("Other") && topic.getDescription().equals("No encontramos un topico")) {
                //         ne.prettyPrint();
                //     }
                // }

			}
		}
		 else {
			printHelp();
		}
	}
}
