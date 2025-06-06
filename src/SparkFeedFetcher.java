import scala.Tuple2;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import feed.Article;
import feed.Feed;
import httpRequest.HttpRequester;
import namedEntity.NamedEntity;
import namedEntity.heuristic.Heuristic;
import namedEntity.heuristic.QuickHeuristic;
import namedEntity.heuristic.RandomHeuristic;
import parser.RssParser;
import parser.SubscriptionParser;
import subscription.SingleSubscription;
import subscription.Subscription;

/**
 * Clase principal que implementa el procesamiento distribuido de feeds RSS usando Apache Spark.
 * 
 * Esta clase reemplaza la funcionalidad secuencial del FeedReaderMain original,
 * aplicando paralelización en dos niveles:
 * 1. Descarga y parseo de feeds (un worker por feed)
 * 2. Procesamiento de entidades nombradas (un worker por artículo)
 */
public class SparkFeedFetcher {
    
    public static void main(String[] args) {
        System.out.println("************* SparkFeedFetcher version 1.0 *************");
        
        // Configurar Spark Session
        SparkSession spark = SparkSession
            .builder()
            .appName("FeedFetcher")
            .master("local[*]")  // Modo local para desarrollo, cambiar para cluster
            .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
            .getOrCreate();
        
        try {
            // Procesar feeds usando Spark
            processFeeds(spark, args);
        } catch (Exception e) {
            System.err.println("Error en el procesamiento: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Siempre cerrar la sesión de Spark
            spark.stop();
        }
    }
    
    /**
     * Método principal que coordina todo el pipeline de procesamiento distribuido
     */
    private static void processFeeds(SparkSession spark, String[] args) throws Exception {
        // 1. Leer configuración (secuencial, archivo pequeño)
        SubscriptionParser subParser = new SubscriptionParser("./config/subscriptions.json");
        Subscription allSubscriptions = subParser.getSubscriptions();
        
        // 2. Expandir URLs con parámetros
        List<String> allUrls = expandUrls(allSubscriptions);
        System.out.println("Total URLs a procesar: " + allUrls.size());
        
        // 3. PRIMERA PARALELIZACIÓN: Descargar y parsear feeds
        JavaRDD<Feed> feedsRDD = downloadAndParseFeeds(spark, allUrls);
        
        // 4. SEGUNDA PARALELIZACIÓN: Procesar entidades nombradas
        processNamedEntitiesDistributed(feedsRDD, args);
    }
    
    /**
     * Expande las URLs base con sus parámetros para crear la lista completa de URLs a procesar
     */
    private static List<String> expandUrls(Subscription subscription) {
        List<String> allUrls = new ArrayList<>();
        
        for (SingleSubscription sub : subscription.getSubscriptionsList()) {
            String baseUrl = sub.getUrl();
            List<String> params = sub.getUrlParams();
            String urlType = sub.getUrlType();
            
            // Solo procesar RSS según el enunciado
            if (!"rss".equalsIgnoreCase(urlType)) {
                System.out.println("Saltando URL no-RSS: " + baseUrl);
                continue;
            }
            
            for (String param : params) {
                String finalUrl = baseUrl.replaceAll("%s", param);
                allUrls.add(finalUrl);
            }
        }
        
        return allUrls;
    }
    
    /**
     * PRIMERA PARALELIZACIÓN: Distribuye la descarga y parseo de feeds entre workers
     * 
     * ¿Por qué paralelizar aquí?
     * - Las descargas HTTP son lentas e independientes entre sí
     * - Cada URL puede procesarse en un worker diferente
     * - Mejora significativa en tiempo total de descarga
     */
    private static JavaRDD<Feed> downloadAndParseFeeds(SparkSession spark, List<String> urls) {
        // Crear RDD distribuido de URLs usando JavaSparkContext
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        JavaRDD<String> urlsRDD = jsc.parallelize(urls, Math.min(urls.size(), 10)); // Máximo 10 particiones
        
        // Mapear cada URL a un Feed usando workers distribuidos
        JavaRDD<Feed> feedsRDD = urlsRDD.map(url -> {
            try {
                // crear objetos dentro del worker para evitar problemas de serializacion
                HttpRequester requester = new HttpRequester();
                String content = requester.getFeedRss(url);
                
                // Verificar que el contenido es realmente un feed RSS/XML
                if (content == null || content.trim().isEmpty()) {
                    System.err.println("Contenido vacío para URL: " + url);
                    return new Feed("ERROR_EMPTY_" + url);
                }
                
                // Verificar que es XML válido (RSS o RSS+Atom)
                if (!isValidRssFeed(content)) {
                    System.err.println("Contenido no es RSS válido para URL: " + url);
                    return new Feed("ERROR_NOT_RSS_" + url);
                }
                
                InputStream xmlStream = new ByteArrayInputStream(
                    content.getBytes(StandardCharsets.UTF_8)
                );
                RssParser parser = new RssParser(xmlStream, url);
                Feed feed = parser.parseFeed();
                
                System.out.println("Feed procesado: " + feed.getSiteName() + 
                                 " (" + feed.getNumberOfArticles() + " artículos)");
                return feed;

            } catch (Exception e) {
                System.err.println("Error procesando URL: " + url + " - " + e.getMessage());
                // retornar feed vacio que sera filtrado
                return new Feed("ERROR_EXCEPTION_" + url);
            }
        });
        
        // Filtrar feeds que fallaron y retornar
        JavaRDD<Feed> validFeeds = feedsRDD.filter(feed -> 
            !feed.getSiteName().startsWith("ERROR_") && 
            feed.getNumberOfArticles() > 0
        );
        
        // Nota: JavaSparkContext se cierra automáticamente cuando se cierra SparkSession
        return validFeeds;
    }
    
    /**
     * SEGUNDA PARALELIZACIÓN: Distribuye el procesamiento de entidades nombradas
     * 
     * ¿Por qué paralelizar aquí?
     * - El procesamiento de texto es CPU-intensivo
     * - Cada artículo puede procesarse independientemente
     * - Escala bien con el número de artículos
     */
    private static void processNamedEntitiesDistributed(JavaRDD<Feed> feedsRDD, String[] args) {
        // Determinar qué heurística usar
        String heuristicType = (args.length > 0) ? args[0] : "-qh";
        
        // Aplanar feeds a artículos individuales
        JavaRDD<Article> articlesRDD = feedsRDD.flatMap(feed -> 
            feed.getArticleList().iterator()
        );
        
        System.out.println("Total artículos a procesar: " + articlesRDD.count());
        
        // Procesar entidades de cada artículo en paralelo
        JavaRDD<NamedEntity> entitiesRDD = articlesRDD.flatMap(article -> {
            // CRÍTICO: Crear heurística dentro del worker
            Heuristic heuristic;
            if ("-rh".equals(heuristicType)) {
                heuristic = new RandomHeuristic();
            } else {
                heuristic = new QuickHeuristic();
            }
            
            // Procesar entidades del artículo
            article.computeNamedEntities(heuristic);
            return article.getNamedEntityList().iterator();
        });
        
        // Agregar y contar entidades
        countAndDisplayEntities(entitiesRDD);
    }
    
    /**
     * Cuenta y muestra las entidades nombradas usando MapReduce de Spark
     * 
     * ¿Por qué este enfoque?
     * - reduceByKey() es la operación MapReduce clásica para conteos
     * - Spark optimiza automáticamente esta operación
     * - Maneja grandes volúmenes de datos eficientemente
     */
    private static void countAndDisplayEntities(JavaRDD<NamedEntity> entitiesRDD) {
        // Crear pares (nombre_entidad, 1) para el conteo MapReduce
        JavaPairRDD<String, Integer> entityPairs = entitiesRDD.mapToPair(entity -> 
            new Tuple2<>(entity.getName(), 1)
        );
        
        // Contar ocurrencias usando reduceByKey (operación distribuida)
        JavaPairRDD<String, Integer> entityCounts = entityPairs.reduceByKey((a, b) -> a + b);
        
        // Ordenar por frecuencia descendente
        List<Tuple2<String, Integer>> results = entityCounts
            .mapToPair(tuple -> new Tuple2<>(tuple._2(), tuple._1())) // Invertir para ordenar por count
            .sortByKey(false) // Orden descendente
            .mapToPair(tuple -> new Tuple2<>(tuple._2(), tuple._1())) // Volver al formato original
            .collect(); // Traer resultados al driver
        
        // Mostrar resultados
        System.out.println("\n=== ENTIDADES NOMBRADAS ENCONTRADAS ===");
        System.out.println("Total entidades únicas: " + results.size());
        System.out.println("Formato: <entidad nombrada>: <conteo>\n");
        
        for (Tuple2<String, Integer> result : results) {
            System.out.println(result._1() + ": " + result._2());
        }
    }
    
    /**
     * Verifica si el contenido es un feed RSS válido (incluyendo RSS con elementos Atom)
     */
    private static boolean isValidRssFeed(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String normalizedContent = content.toLowerCase().trim();
        
        // Verificar que es XML
        if (!normalizedContent.startsWith("<?xml") && !normalizedContent.contains("<rss")) {
            return false;
        }
        
        // Verificar que tiene elementos RSS básicos
        boolean hasRssTag = normalizedContent.contains("<rss") || normalizedContent.contains("<rss>");
        boolean hasChannelTag = normalizedContent.contains("<channel");
        boolean hasItemTags = normalizedContent.contains("<item");
        
        // También verificar si tiene elementos Atom (feeds híbridos)
        boolean hasAtomNamespace = normalizedContent.contains("xmlns:atom") || 
                                  normalizedContent.contains("atom:");
        
        // Es válido si:
        // 1. Tiene estructura RSS básica (rss + channel + items), O
        // 2. Tiene elementos RSS con namespace Atom (feeds híbridos)
        return (hasRssTag && hasChannelTag && hasItemTags) || 
               (hasRssTag && hasAtomNamespace && hasItemTags);
    }
}
