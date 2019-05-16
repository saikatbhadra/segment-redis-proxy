FROM maven:3.5-jdk-8 AS build  
COPY src /usr/src/app/src  
COPY pom.xml /usr/src/app  
RUN mvn -f /usr/src/app/pom.xml clean package

FROM java:8
COPY --from=build /usr/src/app/target/redis-proxy-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/app/redis-proxy-1.0-SNAPSHOT-jar-with-dependencies.jar
#EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/app/redis-proxy-1.0-SNAPSHOT-jar-with-dependencies.jar"]