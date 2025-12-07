FROM eclipse-temurin:21-jre-alpine AS builder
WORKDIR /workspace
COPY . .
RUN ./mvnw clean package -DskipTests || mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="sphinx46"
WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
EXPOSE ${PORT:-8080}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]