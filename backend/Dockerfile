FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
