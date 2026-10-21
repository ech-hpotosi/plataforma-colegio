# Imagen del backend para el servidor. Se construye desde despliegue/docker-compose.yml.
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /fuente
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q -DskipTests package && cp target/plataforma-*.jar /fuente/plataforma.jar

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 plataforma \
    && mkdir -p /datos/archivos \
    && chown -R plataforma /datos
WORKDIR /app
COPY --from=construccion /fuente/plataforma.jar plataforma.jar
USER plataforma
ENV ARCHIVOS_DIRECTORIO=/datos/archivos \
    SPRING_PROFILES_ACTIVE=produccion \
    JAVA_TOOL_OPTIONS="-Duser.timezone=America/Bogota -XX:MaxRAMPercentage=60"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/plataforma.jar"]
