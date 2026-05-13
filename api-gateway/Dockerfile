# ========================================================
# STAGE 1: BUILD (Ambiente di compilazione)
# ========================================================
# Utilizziamo l'immagine ufficiale Maven con Eclipse Temurin per Java 25
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

# Creiamo la cartella di lavoro all'interno del container
WORKDIR /build

# --- OTTIMIZZAZIONE DELLA CACHE DI DOCKER ---
# Copiamo PRIMA solo il pom.xml e scarichiamo le dipendenze offline.
# Docker mette in cache questo livello. Se modifichi solo il codice Java (src), 
# Docker non riscaricherà tutte le librerie da capo, risparmiando minuti preziosi.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Ora copiamo il codice sorgente
COPY src ./src

# Compiliamo il progetto generando il .jar.
# Saltiamo i test unitari perché in K8s la build dell'immagine deve essere 
# veloce e agnostica rispetto ai database di test.
RUN mvn clean package -DskipTests

# ========================================================
# STAGE 2: RUN (Ambiente di esecuzione per la Produzione)
# ========================================================
# Ripariamo da un'immagine pulita e leggerissima (solo JRE/JDK)
FROM eclipse-temurin:25-jdk-alpine

# --- BEST PRACTICE DI SICUREZZA KUBERNETES ---
# Non eseguire MAI l'applicazione come utente 'root' all'interno del container.
# Creiamo un utente di sistema limitato chiamato 'spring'.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copiamo SOLO il file .jar finale dallo Stage 1 ('builder').
# Tutto il peso di Maven e delle dipendenze scaricate (cache di .m2) viene scartato.
COPY --from=builder /build/target/*.jar app.jar

# Documentiamo la porta su cui il container è in ascolto (L'API Gateway usa la 8080)
EXPOSE 8080

# Avvio dell'applicazione
ENTRYPOINT ["java", "-jar", "app.jar"]