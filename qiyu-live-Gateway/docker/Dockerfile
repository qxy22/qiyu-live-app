FROM eclipse-temurin:17-jre-alpine

VOLUME /tmp
EXPOSE 8086

ADD qiyu-live-gateway-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
