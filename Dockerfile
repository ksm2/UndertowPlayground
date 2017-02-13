FROM openjdk:8-jre
COPY ./target/*-with-dependencies.jar /jars/server.jar
EXPOSE 8080 8443
CMD java -jar /jars/server.jar
