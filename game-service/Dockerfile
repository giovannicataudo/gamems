# Usa la distribuzione Eclipse Temurin per Java 25 (versione Alpine ultraleggera)
FROM eclipse-temurin:25-jdk-alpine

# Crea una directory di lavoro
WORKDIR /app

# Copia il file .jar generato da Maven all'interno del container
COPY target/*.jar app.jar

# Comando di esecuzione
ENTRYPOINT ["java", "-jar", "app.jar"]