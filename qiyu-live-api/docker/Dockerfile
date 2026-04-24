FROM eclipse-temurin:17-jre-alpine

VOLUME /tmp
EXPOSE 8083

ADD qiyu-live-api-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
