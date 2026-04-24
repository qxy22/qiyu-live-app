FROM eclipse-temurin:17-jre-alpine

VOLUME /tmp
EXPOSE 8085 9090

ADD qiyu-live-user-provider-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
