# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Requires feature-flag-sdk-java to already be `mvn install`-ed into the
# local repo this build has access to (see README).
COPY pom.xml .
RUN mvn -B dependency:go-offline || true
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
