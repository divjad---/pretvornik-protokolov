FROM eclipse-temurin:17.0.10_7-jre

WORKDIR /app

COPY ./transformation-framework/target/transformation-framework-1.0-SNAPSHOT.jar .
COPY ./log-config/log4j2.xml ./log-config/log4j2.xml

ARG API-KEY
ENV KUMULUZEE_LOGS_CONFIGFILELOCATION=./log-config/log4j2.xml API-KEY=$API-KEY

ENTRYPOINT ["java", "-jar", "transformation-framework-1.0-SNAPSHOT.jar"]

EXPOSE 9094