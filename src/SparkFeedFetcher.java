import scala.Tuple2;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
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

public class SparkFeedFetcher {
    
    public static void main(String[] args) {
        // Suppress Spark's verbose INFO logs for a cleaner console output
        Logger.getLogger("org").setLevel(Level.ERROR);
        Logger.getLogger("akka").setLevel(Level.ERROR);

        System.out.println("** SparkFeedFetcher version 1.0 **");
        
        SparkSession spark = SparkSession
            .builder()
            .appName("FeedFetcher")
            .master("local[*]")
            .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
            .getOrCreate();
        
        try {
            processFeeds(spark, args);
        } catch (Exception e) {
            System.err.println("Error en el procesamiento: " + e.getMessage());
            e.printStackTrace();
        } finally {
            spark.stop();
        }
    }
    
    private static void processFeeds(SparkSession spark, String[] args) throws Exception {
        SubscriptionParser subParser = new SubscriptionParser("./config/subscriptions.json");
        Subscription allSubscriptions = subParser.getSubscriptions();
        
        List<String> allUrls = expandUrls(allSubscriptions);
        
        JavaRDD<Feed> feedsRDD = downloadAndParseFeeds(spark, allUrls);
        
        processNamedEntitiesDistributed(feedsRDD, args);
    }
    
    private static List<String> expandUrls(Subscription subscription) {
        List<String> allUrls = new ArrayList<>();
        
        for (SingleSubscription sub : subscription.getSubscriptionsList()) {
            String baseUrl = sub.getUrl();
            List<String> params = sub.getUrlParams();
            
            for (String param : params) {
                String finalUrl = baseUrl.replaceAll("%s", param);
                allUrls.add(finalUrl);
            }
        }
        return allUrls;
    }
    
    private static JavaRDD<Feed> downloadAndParseFeeds(SparkSession spark, List<String> urls) {
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        JavaRDD<String> urlsRDD = jsc.parallelize(urls, urls.size());

        JavaRDD<Feed> feedsRDD = urlsRDD.map(url -> {
            try {
                HttpRequester requester = new HttpRequester();
                String content = requester.getFeedRss(url);
                
                InputStream xmlStream = new ByteArrayInputStream(
                    content.getBytes(StandardCharsets.UTF_8)
                );
                RssParser parser = new RssParser(xmlStream, url);
                return parser.parseFeed();
            } catch (Exception e) {
                // Return an empty feed on error, which will be filtered out
                return new Feed("ERROR_" + url);
            }
        });
        
        return feedsRDD.filter(feed -> 
            !feed.getSiteName().startsWith("ERROR_") && 
            feed.getNumberOfArticles() > 0
        );
    }
    
    private static void processNamedEntitiesDistributed(JavaRDD<Feed> feedsRDD, String[] args) {
        String heuristicType = (args.length > 0) ? args[0] : "-qh";
        
        JavaRDD<Article> articlesRDD = feedsRDD.flatMap(feed -> 
            feed.getArticleList().iterator()
        );
        
        // The .count() call was removed to avoid an unnecessary job and extra logs
        
        JavaRDD<NamedEntity> entitiesRDD = articlesRDD.flatMap(article -> {
            Heuristic heuristic;
            if ("-rh".equals(heuristicType)) {
                heuristic = new RandomHeuristic();
            } else {
                heuristic = new QuickHeuristic();
            }
            
            article.computeNamedEntities(heuristic);
            return article.getNamedEntityList().iterator();
        });
        
        countAndDisplayEntities(entitiesRDD);
    }
    
    private static void countAndDisplayEntities(JavaRDD<NamedEntity> entitiesRDD) {
        JavaPairRDD<String, Integer> entityPairs = entitiesRDD.mapToPair(entity -> 
            new Tuple2<>(entity.getName(), 1)
        );
        
        JavaPairRDD<String, Integer> entityCounts = entityPairs.reduceByKey((a, b) -> a + b);
        
        List<Tuple2<String, Integer>> results = entityCounts
            .mapToPair(tuple -> new Tuple2<>(tuple._2(), tuple._1()))
            .sortByKey(false)
            .mapToPair(tuple -> new Tuple2<>(tuple._2(), tuple._1()))
            .collect();
        
        System.out.println("\n=== ENTIDADES NOMBRADAS ENCONTRADAS ===");
        System.out.println("Total entidades únicas: " + results.size());
        
        for (Tuple2<String, Integer> result : results) {
            if(result._2()>10){

                System.out.println(result._1() + ": " + result._2());
            }
        }
    }
}