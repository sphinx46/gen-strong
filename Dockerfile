FROM eclipse-temurin:21-jdk-jammy
LABEL maintainer="sphinx46"
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Dspring.profiles.active=${APP_PROFILE:-dev}", "-jar", "/app/app.jar"]