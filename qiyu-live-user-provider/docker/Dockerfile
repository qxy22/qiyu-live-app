FROM openjdk:17-jdk-alpine

VOLUME /tmp
EXPOSE 8082 9090

ADD qiyu-live-user-provider-docker.jar app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
