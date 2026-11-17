# syntax=docker/dockerfile:1
# Maven 3.9 + Eclipse Temurin 17 (build y JRE 17 runtime).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package \
    && count="$(find /app/target -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.jar.original' | wc -l | tr -d ' ')" \
    && if [ "$count" -eq 0 ]; then \
         echo "ERROR: no se encontro ningun JAR ejecutable en /app/target (se excluyen *-sources.jar, *-javadoc.jar y *.jar.original)" >&2; exit 1; \
       fi \
    && if [ "$count" -gt 1 ]; then \
         echo "ERROR: se encontraron varios JAR ejecutables en /app/target; debe quedar exactamente uno:" >&2; \
         find /app/target -maxdepth 1 -type f -name '*.jar' \
           ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.jar.original' -print >&2; exit 1; \
       fi \
    && f="$(find /app/target -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' ! -name '*.jar.original' -print -quit)" \
    && cp "$f" /app/application.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# UID/GID fijos para permisos del bind mount de logs en el host (ver docker-compose.yml).
RUN groupadd --gid 10001 spring \
    && useradd --uid 10001 --gid spring --system --no-create-home --shell /usr/sbin/nologin spring

COPY --from=build /app/application.jar /app/application.jar
RUN mkdir -p /app/logs && chown spring:spring /app/application.jar /app/logs

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
