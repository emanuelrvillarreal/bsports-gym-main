FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --gid 10001 spring \
    && useradd --uid 10001 --gid spring --system --no-create-home --shell /usr/sbin/nologin spring

COPY application.jar /app/application.jar
RUN mkdir -p /app/logs && chown spring:spring /app/application.jar /app/logs

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
