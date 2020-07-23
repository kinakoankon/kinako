FROM openjdk:11


ADD app-1.0.0.jar app.jar
EXPOSE 80
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=85", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=80", "-jar", "/app.jar"]