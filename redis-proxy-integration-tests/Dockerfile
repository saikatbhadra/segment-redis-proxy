FROM java:8
WORKDIR /app
COPY src ./src
COPY lib ./lib
RUN javac -cp "lib/*:." src/RedisProxyIT.java
ENTRYPOINT java -cp "lib/*:." org.junit.runner.JUnitCore src.RedisProxyIT