# Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=build /app/target/content-pubsub-system-1.0-SNAPSHOT.jar app.jar

# Create volume for H2 data
VOLUME /app/data

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=docker", "-Dspring.main.web-application-type=NONE", "app.jar"]