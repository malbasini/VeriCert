FROM eclipse-temurin:23-jre

# Directory di lavoro dell'app
WORKDIR /app

# Copia il jar
COPY vericert-0.0.1-SNAPSHOT.jar app.jar

# 👉 Copia anche la cartella con le chiavi
COPY keys ./keys

ENTRYPOINT ["java","-jar","app.jar"]
