# syntax=docker/dockerfile:1.7
# Imagen de producción: compila el JAR y lo ejecuta con un JRE liviano.

FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test \
    && cp build/libs/*-SNAPSHOT.jar app.jar

FROM eclipse-temurin:25-jre-noble
WORKDIR /app

RUN useradd --system --uid 1001 spring
USER spring

COPY --from=build /app/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080

# Si DB_URL no viene definido (p. ej. en Render), se arma desde DB_HOST/DB_PORT/DB_NAME.
ENTRYPOINT ["sh", "-c", "if [ -z \"$DB_URL\" ] && [ -n \"$DB_HOST\" ]; then export DB_URL=\"jdbc:postgresql://$DB_HOST:${DB_PORT:-5432}/$DB_NAME\"; fi; exec java $JAVA_OPTS -jar app.jar"]
