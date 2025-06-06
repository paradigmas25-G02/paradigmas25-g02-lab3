# Laboratorio 3: Procesamiento Distribuido de Feeds RSS con Apache Spark

## Descripción
Este laboratorio extiende el Laboratorio 2 implementando procesamiento distribuido de feeds RSS utilizando Apache Spark. El objetivo es paralelizar tanto la descarga de feeds como el procesamiento de entidades nombradas distribuyendo el trabajo entre múltiples workers.

## Requisitos Implementados

### Distribución de descarga/parsing de feeds
•⁠  ⁠*Un worker por feed*: Cada feed RSS se descarga y parsea en un worker distribuido diferente
•⁠  ⁠*Implementación*: ⁠ JavaRDD<String> urlsRDD = jsc.parallelize(urls, Math.min(urls.size(), 10)) ⁠
•⁠  ⁠*Paralelización*: Máximo 10 particiones para balancear recursos

### Distribución de procesamiento de entidades nombradas  
•⁠  ⁠*Un worker por artículo*: Cada artículo se procesa en un worker distribuido independiente
•⁠  ⁠*Implementación*: ⁠ articlesRDD.flatMap(article -> article.computeNamedEntities(heuristic).iterator()) ⁠
•⁠  ⁠*Escalabilidad*: Procesamiento paralelo de todos los artículos recolectados

### Agregación distribuida de conteos
•⁠  ⁠*MapReduce*: Conteo agregado usando ⁠ reduceByKey() ⁠ sobre todas las entidades
•⁠  ⁠*Implementación*: ⁠ .mapToPair(entity -> new Tuple2<>(entity.getName(), 1)).reduceByKey(Integer::sum) ⁠
•⁠  ⁠*Resultado*: Conteos finales consolidados de todas las fuentes

### Filtrado RSS únicamente
•⁠  ⁠*Solo feeds RSS*: Filtra automáticamente URLs no-RSS (ej: Reddit)
•⁠  ⁠*Implementación*: Verificación de tipo de parser antes de procesar
•⁠  ⁠*Logging*: Informa URLs saltadas para transparencia

## Estructura del Proyecto


├── src/
│   ├── SparkFeedFetcher.java      # Aplicación principal distribuida
│   ├── FeedReaderMain.java        # Versión original secuencial
│   ├── feed/                      # Clases de feed (ahora Serializable)
│   ├── namedEntity/              # Sistema de entidades nombradas
│   └── parser/                   # Parsers RSS/Reddit
├── config/
│   └── subscriptions.json        # Configuración de feeds
├── pom.xml                       # Dependencias Maven (con Spark)
├── Makefile                      # Sistema de build automatizado
└── README.md                     # Este archivo


## Dependencias y Tecnologías

### Apache Spark 3.5.0
•⁠  ⁠*spark-core_2.12*: Motor de procesamiento distribuido
•⁠  ⁠*spark-sql_2.12*: APIs de alto nivel y optimizaciones
•⁠  ⁠*Scala 2.12.18*: Runtime requerido por Spark

### Compatibilidad Java 17
•⁠  ⁠*Opciones JVM*: ⁠ --add-opens ⁠ para acceso a módulos internos
•⁠  ⁠*Maven*: Configuración automática de parámetros de compatibilidad

## Uso

### Compilar y Ejecutar
⁠ bash
# Compilar el proyecto
make compile

# Ejecutar versión Spark distribuida
make run                    # Modo local
make local                  # Modo local explícito

# Ejecutar versión original para comparación
make original

# Comparar rendimiento
make benchmark
 ⁠

### Opciones de Heurística
⁠ bash
make run HEURISTIC=-qh      # QuickHeuristic (por defecto)
make run HEURISTIC=-rh      # RandomHeuristic
 ⁠

## Implementación Técnica

### Clase Principal: SparkFeedFetcher
⁠ java
public class SparkFeedFetcher implements Serializable {
    // Pipeline distribuido de dos niveles:
    // 1. Distribución de feeds -> Workers por feed
    // 2. Distribución de artículos -> Workers por artículo
    // 3. Agregación MapReduce -> Conteos consolidados
}
 ⁠

### Serialización Distribuida
Todas las clases del dominio implementan ⁠ Serializable ⁠ para distribución:
•⁠  ⁠⁠ Article ⁠, ⁠ Feed ⁠, ⁠ NamedEntity ⁠
•⁠  ⁠⁠ Heuristic ⁠, ⁠ Category ⁠, ⁠ Topic ⁠
•⁠  ⁠⁠ serialVersionUID ⁠ para compatibilidad

### Estrategia de Paralelización
1.⁠ ⁠*Nivel 1 - Feeds*: ⁠ urlsRDD.map() ⁠ distribuye descarga/parsing
2.⁠ ⁠*Nivel 2 - Artículos*: ⁠ articlesRDD.flatMap() ⁠ distribuye procesamiento de entidades
3.⁠ ⁠*Nivel 3 - Agregación*: ⁠ reduceByKey() ⁠ consolida conteos finales

## Resultados de Ejemplo

### Versión Spark (Distribuida)

Total URLs a procesar: 2
Total artículos a procesar: 66
Total entidades únicas: 483

Donald Trump: 15
AI: 11
China: 6
Elon Musk: 5
Tesla: 5
...


### Versión Original (Secuencial)

Donald Trump is a Person that appears 24 times and is related to Politics
Amazon is a Organization that appears 8 times and is related to Culture
Elon Musk is a Person that appears 7 times and is related to Culture
...


## Análisis Comparativo

### Rendimiento
•⁠  ⁠*Spark*: ~16.9s (incluye overhead de inicialización distribuida)
•⁠  ⁠*Original*: ~9.7s (procesamiento secuencial optimizado)
•⁠  ⁠*Trade-off*: Mayor latencia por mayor escalabilidad

### Escalabilidad
•⁠  ⁠*Spark*: Escalamiento horizontal automático
•⁠  ⁠*Original*: Limitado por un solo núcleo/hilo
•⁠  ⁠*Beneficio*: Spark escala con datos grandes y clusters

### Precisión
•⁠  ⁠*Ambas versiones*: Procesan las mismas fuentes RSS
•⁠  ⁠*Diferencias*: Formato de salida adaptado a cada paradigma
•⁠  ⁠*Consistencia*: Entidades principales identificadas correctamente

## Preguntas Conceptuales

### 1. ¿Qué ventajas aporta la programación funcional al procesamiento distribuido?

*Inmutabilidad y Funciones Puras:*
•⁠  ⁠Las funciones puras (sin efectos secundarios) son inherentemente thread-safe
•⁠  ⁠La inmutabilidad elimina condiciones de carrera y sincronización compleja
•⁠  ⁠Facilita la distribución automática sin preocuparse por estado compartido

*Composición y Transformaciones:*
•⁠  ⁠Las operaciones map/filter/reduce son naturalmente paralelas
•⁠  ⁠El paradigma funcional mapea directamente a MapReduce distribuido
•⁠  ⁠La composición de funciones permite pipelines de procesamiento claros

*Ejemplo en nuestro código:*
⁠ java
// Pipeline funcional distribuido
urlsRDD
  .map(this::downloadAndParseFeed)     // Transformación pura
  .filter(Objects::nonNull)            // Filtrado sin efectos
  .flatMap(feed -> feed.getArticles()) // Composición natural
  .flatMap(article -> entities)       // Mapeo inmutable
  .mapToPair(entity -> tuple)         // Transformación funcional
  .reduceByKey(Integer::sum)          // Agregación asociativa
 ⁠

### 2. ¿Cómo maneja Spark la distribución de datos y procesamiento?

*RDD (Resilient Distributed Datasets):*
•⁠  ⁠*Particionamiento automático*: Los datos se dividen en particiones distribuidas
•⁠  ⁠*Lazy evaluation*: Las transformaciones se optimizan antes de ejecutarse
•⁠  ⁠*Fault tolerance*: Reconstrucción automática de particiones perdidas

*Distribución de Tareas:*
•⁠  ⁠*Driver program*: Coordina la ejecución y mantiene el DAG
•⁠  ⁠*Executors*: Workers distribuidos que procesan particiones independientes
•⁠  ⁠*Cluster manager*: Gestiona recursos y planifica tareas

*En nuestro proyecto:*
⁠ java
// Particionamiento controlado
JavaRDD<String> urlsRDD = jsc.parallelize(urls, Math.min(urls.size(), 10));

// Transformaciones lazy (no se ejecutan hasta collect())
JavaRDD<NamedEntity> entitiesRDD = urlsRDD
    .map(this::downloadAndParseFeed)
    .flatMap(feed -> feed.getArticles().stream().iterator())
    .flatMap(article -> article.computeNamedEntities(heuristic).iterator());

// Acción que dispara la ejecución distribuida
List<Tuple2<String, Integer>> results = entitiesRDD
    .mapToPair(entity -> new Tuple2<>(entity.getName(), 1))
    .reduceByKey(Integer::sum)
    .collect(); // ← Aquí se ejecuta todo el pipeline
 ⁠

### 3. ¿Qué estrategias de paralelización son más efectivas para diferentes tipos de datos?

*Datos Independientes (Feed URLs):*
•⁠  ⁠*Estrategia*: Paralelización por elemento (⁠ parallelize() ⁠)
•⁠  ⁠*Particionamiento*: Una partición por feed para balancear E/O de red
•⁠  ⁠*Beneficio*: Máximo paralelismo sin dependencias

*Datos Jerárquicos (Articles dentro de Feeds):*
•⁠  ⁠*Estrategia*: ⁠ flatMap() ⁠ para aplanar y redistribuir
•⁠  ⁠*Reparticionamiento*: Redistribución automática para balance de carga
•⁠  ⁠*Beneficio*: Granularidad fina de procesamiento

*Agregaciones (Entity counting):*
•⁠  ⁠*Estrategia*: MapReduce con ⁠ reduceByKey() ⁠
•⁠  ⁠*Combiners locales*: Agregación parcial antes de shuffle
•⁠  ⁠*Beneficio*: Minimiza transferencia de datos entre nodos

*En nuestro diseño:*
⁠ java
// Paralelización por feed (independiente)
JavaRDD<Feed> feedsRDD = urlsRDD.map(url -> downloadFeed(url));

// Redistribución por artículo (jerárquico → plano)
JavaRDD<Article> articlesRDD = feedsRDD.flatMap(feed -> 
    feed.getArticles().stream().iterator());

// Agregación distribuida con combiners
JavaPairRDD<String, Integer> countsRDD = entitiesRDD
    .mapToPair(entity -> new Tuple2<>(entity.getName(), 1))
    .reduceByKey(Integer::sum); // Combiners automáticos
 ⁠

### 4. ¿Cuáles son las principales diferencias entre el modelo de actores y el paradigma funcional distribuido?

*Modelo de Actores:*
•⁠  ⁠*Estado*: Cada actor mantiene estado mutable encapsulado
•⁠  ⁠*Comunicación*: Mensajes asincrónicos entre actores
•⁠  ⁠*Concurrencia*: Actores procesando independientemente
•⁠  ⁠*Ejemplo*: Akka, Erlang/OTP

*Paradigma Funcional Distribuido:*
•⁠  ⁠*Estado*: Datos inmutables y transformaciones puras
•⁠  ⁠*Comunicación*: Paso de datos a través de transformaciones
•⁠  ⁠*Concurrencia*: Paralelización automática de operaciones
•⁠  ⁠*Ejemplo*: Spark, MapReduce

*Comparación Práctica:*

| Aspecto | Actores | Funcional |
|---------|---------|-----------|
| *Mutabilidad* | Estado mutable encapsulado | Datos inmutables |
| *Comunicación* | Mensajes asincrónicos | Transformaciones de datos |
| *Escalabilidad* | Manual (crear/supervisar actores) | Automática (particionamiento) |
| *Tolerancia a fallos* | Supervisión y restart | Recomputación de lineage |
| *Debugging* | Complejo (mensajes asincrónicos) | Determinista (funciones puras) |

*En nuestro contexto RSS:*
•⁠  ⁠*Con Actores*: Un actor por feed, mensajes de artículos, estado de conteos
•⁠  ⁠*Con Spark*: Transformaciones puras, inmutabilidad, agregación distributiva

## Configuración y Dependencias

### Java 17 + Spark Compatibility
⁠ xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <configuration>
        <options>
            <option>--add-opens</option>
            <option>java.base/sun.nio.ch=ALL-UNNAMED</option>
            <!-- Más opciones para compatibilidad -->
        </options>
    </configuration>
</plugin>
 ⁠

### Maven Dependencies
⁠ xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-core_2.12</artifactId>
    <version>3.5.0</version>
</dependency>
 ⁠

## Autores
•⁠  ⁠*Grupo*: paradigmas25-g02
•⁠  ⁠*Laboratorio*: 3 - Procesamiento Distribuido con Apache Spark
•⁠  ⁠*Fecha*: Junio 2025

## Conclusiones

La implementación exitosa demuestra que:

1.⁠ ⁠*Apache Spark permite paralelizar efectivamente* el procesamiento de feeds RSS
2.⁠ ⁠*La programación funcional facilita la distribución* sin complejidad de concurrencia manual  
3.⁠ ⁠*El modelo MapReduce es ideal* para agregación de datos distribuidos
4.⁠ ⁠*Java 17 + Spark 3.5 son compatibles* con la configuración adecuada
5.⁠ ⁠*El trade-off latencia/escalabilidad* es fundamental en sistemas distribuidos

El proyecto implementa exitosamente todos los requisitos de distribución, proporciona comparación con la versión secuencial, y demuestra los principios fundamentales del procesamiento distribuido de big data.