# Laboratorio 3 - RSS Feed Processing with Apache Spark
# Makefile para compilación y ejecución distribuida con Maven

# Configuración de directorios
SRC_DIR = src
TARGET_DIR = target
CONFIG_DIR = config

# Configuración del proyecto
MAIN_CLASS = SparkFeedFetcher
ORIGINAL_MAIN = FeedReaderMain

# Parámetros por defecto
HEURISTIC ?= -qh
SPARK_MASTER ?= local[*]

# Configuración de opciones JVM para Java 17 + Spark
JAVA_OPTS = --add-opens java.base/sun.nio.ch=ALL-UNNAMED \
           --add-opens java.base/java.nio=ALL-UNNAMED \
           --add-opens java.base/java.util=ALL-UNNAMED \
           --add-opens java.base/java.lang.invoke=ALL-UNNAMED \
           --add-opens java.base/java.lang.reflect=ALL-UNNAMED \
           --add-opens java.base/java.net=ALL-UNNAMED \
           --add-opens java.base/java.io=ALL-UNNAMED \
           -Djava.security.manager=allow

.PHONY: all clean compile run local cluster help original maven-compile

# Target por defecto
all: compile

# Ayuda - explica todos los comandos disponibles
help:
	@echo "=== Laboratorio 3 - Makefile Help ==="
	@echo "Comandos disponibles:"
	@echo "  make compile    - Compilar código Java con Maven y dependencias de Spark"
	@echo "  make run        - Ejecutar SparkFeedFetcher en modo local (por defecto)"
	@echo "  make local      - Ejecutar en modo local con todos los cores"
	@echo "  make original   - Ejecutar FeedReaderMain original (secuencial)"
	@echo "  make clean      - Limpiar archivos compilados"
	@echo "  make jar        - Crear JAR ejecutable para distribución"
	@echo "  make benchmark  - Comparar versión original vs Spark"
	@echo ""
	@echo "Parámetros opcionales:"
	@echo "  HEURISTIC={-qh|-rh}  - Tipo de heurística (QuickHeuristic o RandomHeuristic)"
	@echo "  SPARK_MASTER=url     - URL del cluster Spark (ej: spark://localhost:7077)"
	@echo ""
	@echo "Ejemplos:"
	@echo "  make run HEURISTIC=-rh"
	@echo "  make benchmark HEURISTIC=-qh"

# Compilar código Java con Maven
compile: maven-compile
	@echo "✓ Compilación completada con Maven"

maven-compile:
	@echo "=== Compilando con Maven (incluye dependencias de Spark) ==="
	mvn clean compile

# Ejecutar SparkFeedFetcher en modo local (desarrollo)
run: compile
	@echo "=== Ejecutando SparkFeedFetcher en modo LOCAL ==="
	@echo "Heurística: $(HEURISTIC)"
	MAVEN_OPTS="$(JAVA_OPTS)" mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="$(HEURISTIC)"

# Ejecutar en modo local explícito
local: compile
	@echo "=== Ejecutando en modo LOCAL con todos los cores ==="
	SPARK_MASTER="local[*]" MAVEN_OPTS="$(JAVA_OPTS)" mvn exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="$(HEURISTIC)"

# Ejecutar versión original (para comparación)
original: compile
	@echo "=== Ejecutando FeedReaderMain original (secuencial) ==="
	@echo "Heurística: $(HEURISTIC)"
	mvn exec:java -Dexec.mainClass="$(ORIGINAL_MAIN)" -Dexec.args="$(HEURISTIC)"

# Crear JAR para distribución
jar: compile
	@echo "=== Creando JAR ejecutable con Maven ==="
	mvn package
	@echo "✓ JAR creado: $(TARGET_DIR)/paradigmas25-g02-lab2-1.0-SNAPSHOT.jar"

# Benchmark comparativo (secuencial vs distribuido)
benchmark: compile
	@echo "=== Ejecutando benchmark comparativo ==="
	@echo "1. Versión original (secuencial):"
	time mvn -q exec:java -Dexec.mainClass="$(ORIGINAL_MAIN)" -Dexec.args="$(HEURISTIC)"
	@echo ""
	@echo "2. Versión Spark (distribuida):"
	time MAVEN_OPTS="$(JAVA_OPTS)" mvn -q exec:java -Dexec.mainClass="$(MAIN_CLASS)" -Dexec.args="$(HEURISTIC)"

# Limpiar archivos compilados
clean:
	@echo "=== Limpiando archivos compilados ==="
	mvn clean
	@echo "✓ Limpieza completada"

# Debug - mostrar información del entorno
debug:
	@echo "=== Información de debug ==="
	@echo "Java version:"
	java -version
	@echo ""
	@echo "Maven version:"
	mvn -version
	@echo ""
	@echo "Archivos Java encontrados:"
	@find $(SRC_DIR) -name "*.java" | head -10
	@echo ""
	@echo "Configuración actual:"
	@echo "MAIN_CLASS: $(MAIN_CLASS)"
	@echo "HEURISTIC: $(HEURISTIC)"
	@echo "SPARK_MASTER: $(SPARK_MASTER)"

