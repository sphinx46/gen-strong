FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
COPY db/migrations db/migrations
RUN mvn clean package -DskipTests -Dspring.profiles.active=prod -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache tzdata
COPY --from=builder /workspace/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -Duser.timezone=UTC"
ENV PORT=8080

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]