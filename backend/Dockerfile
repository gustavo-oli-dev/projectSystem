# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S sistema && adduser -S sistema -G sistema
WORKDIR /app
COPY --from=build /app/target/sistema-backend-*.jar app.jar
USER sistema
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
