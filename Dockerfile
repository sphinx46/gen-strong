FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-jammy
LABEL maintainer="sphinx46"
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE ${PORT:-8081}
ENTRYPOINT ["java", "-Dserver.port=${PORT:-8081}", "-Dspring.profiles.active=${APP_PROFILE:-dev}", "-jar", "/app/app.jar"]