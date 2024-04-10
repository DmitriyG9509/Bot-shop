FROM openjdk:17-jdk-slim
# port Java app listens to (inside container)
EXPOSE 8080
# install Spring Boot artifact
VOLUME /tmp
ADD target/sweet-bot-0.0.1-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app.jar'
# сonfigure to Almaty timezone. I dislike debugging failures in UTC
RUN unlink /etc/localtime && ln -s /usr/share/zoneinfo/Asia/Almaty /etc/localtime
# copy resources folder to access files (Utils, FileUtils methods)
COPY src/main/resources resources
ENV JAVA_OPTS=""
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar -Xmx8g /app.jar
